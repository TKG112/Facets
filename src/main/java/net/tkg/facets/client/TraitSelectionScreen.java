package net.tkg.facets.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.tkg.facets.trait.TraitHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tkg.facets.network.SubmitSelectionPayload;
import net.tkg.facets.client.theme.FacetsTheme;
import net.tkg.facets.trait.Occupation;
import net.tkg.facets.trait.SelectionContext;
import net.tkg.facets.trait.SelectionMode;
import net.tkg.facets.trait.SelectionRules;
import net.tkg.facets.trait.SortOrder;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitCategory;
import net.tkg.facets.trait.TraitRef;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class TraitSelectionScreen extends Screen implements EntryListWidget.Host<TraitEntry> {
    private static final int LIST_TOP = 44;
    private static final int LABEL_SPACE = 14;
    private static final int MIN_GROUP_HEIGHT = 50;
    private static final int MAX_LIST_WIDTH = 200;
    private static final int COLUMN_GAP = 32;

    private static final Comparator<TraitEntry> BY_NAME = Comparator.comparing(e -> e.trait().name().getString());
    /**
     * Highest cost first. With positive traits on top and negative traits below, the biggest values
     * end up at the outer edges and the cheapest ones meet in the middle.
     */
    private static final Comparator<TraitEntry> BY_COST_DESCENDING = Comparator
            .comparingInt((TraitEntry e) -> -e.trait().cost()).thenComparing(BY_NAME);
    private static final Comparator<TraitEntry> BY_COST_ASCENDING = Comparator
            .comparingInt((TraitEntry e) -> e.trait().cost()).thenComparing(BY_NAME);

    /** One list on the left side: a category, positive/negative traits, or everything. */
    private record Group(Component label, @Nullable TextColor color, List<TraitEntry> entries) {
    }

    private final SelectionRules rules;
    private final SelectionContext context;
    @Nullable
    private final ResourceLocation occupationId;
    @Nullable
    private final Occupation occupation;
    private final List<ResourceLocation> granted;
    private final List<TraitEntry> free = new ArrayList<>();
    private final Map<ResourceLocation, TraitEntry> byId = new HashMap<>();
    private final Map<ResourceLocation, TextColor> groupColors = new HashMap<>();
    private final List<Group> groups = new ArrayList<>();
    private final List<ResourceLocation> chosen = new ArrayList<>();

    /** One list per group when they fit, otherwise a single list showing {@link #page}. */
    private final List<EntryListWidget<TraitEntry>> groupLists = new ArrayList<>();
    private boolean paged;
    private int page;
    private EntryListWidget<TraitEntry> chosenList;
    private Button addButton;
    private Button removeButton;
    private Button clearButton;
    private Button confirmButton;

    @Nullable
    private TraitEntry selected;
    private EntryListWidget.Side selectedSide = EntryListWidget.Side.AVAILABLE;
    @Nullable
    private Component validationError;
    @Nullable
    private Component serverError;
    private boolean waiting;

    public TraitSelectionScreen(SelectionRules rules, SelectionContext context, @Nullable ResourceLocation occupationId,
                                List<ResourceLocation> preselected, List<ResourceLocation> granted) {
        super(Component.translatable("facets.screen.title"));
        this.rules = rules;
        this.context = context;
        this.occupationId = occupationId;
        this.occupation = context.occupation(occupationId);
        this.granted = granted;

        for (TraitHolder holder : context.freeTraits(occupation)) {
            free.add(new TraitEntry(holder));
        }
        for (TraitHolder holder : context.allTraits()) {
            TraitEntry entry = new TraitEntry(holder);
            boolean isFree = free.stream().anyMatch(f -> f.id().equals(entry.id()));
            if (holder.value().selectable() && !isFree && holder.value().availableFor(holder, occupationId, occupation)) {
                byId.put(entry.id(), entry);
            }
        }
        buildGroups();

        for (ResourceLocation id : preselected) {
            if (byId.containsKey(id) && !chosen.contains(id)) {
                chosen.add(id);
            }
        }
    }

    private void buildGroups() {
        boolean points = rules.mode() == SelectionMode.POINTS;
        Comparator<TraitEntry> defaultOrder = points ? BY_COST_DESCENDING : BY_NAME;

        if (!context.categories().isEmpty()) {
            Map<ResourceLocation, List<TraitEntry>> byCategory = new LinkedHashMap<>();
            context.categories().keySet().forEach(id -> byCategory.put(id, new ArrayList<>()));
            List<TraitEntry> other = new ArrayList<>();
            for (TraitEntry entry : byId.values()) {
                Optional<ResourceLocation> category = entry.trait().category().filter(byCategory::containsKey);
                category.ifPresentOrElse(id -> byCategory.get(id).add(entry), () -> other.add(entry));
            }
            context.categories().forEach((id, category) -> {
                List<TraitEntry> entries = byCategory.get(id);
                entries.sort(category.sort().map(TraitSelectionScreen::comparator).orElse(defaultOrder));
                addGroup(category.name(), category.color().orElse(null), entries);
            });
            if (!other.isEmpty()) {
                other.sort(defaultOrder);
                addGroup(Component.translatable("facets.screen.other"), null, other);
            }
            return;
        }

        boolean split = points && byId.values().stream().anyMatch(e -> e.trait().cost() < 0);
        if (split) {
            List<TraitEntry> positive = new ArrayList<>(byId.values().stream().filter(e -> e.trait().cost() >= 0).toList());
            List<TraitEntry> negative = new ArrayList<>(byId.values().stream().filter(e -> e.trait().cost() < 0).toList());
            positive.sort(defaultOrder);
            negative.sort(defaultOrder);
            addGroup(Component.translatable("facets.screen.positive"), null, positive);
            addGroup(Component.translatable("facets.screen.negative"), null, negative);
        } else {
            List<TraitEntry> all = new ArrayList<>(byId.values());
            all.sort(defaultOrder);
            addGroup(Component.translatable("facets.screen.available"), null, all);
        }
    }

    private void addGroup(Component label, @Nullable TextColor color, List<TraitEntry> entries) {
        groups.add(new Group(label, color, entries));
        if (color != null) {
            entries.forEach(e -> groupColors.put(e.id(), color));
        }
    }

    private static Comparator<TraitEntry> comparator(SortOrder order) {
        return switch (order) {
            case COST_DESCENDING -> BY_COST_DESCENDING;
            case COST_ASCENDING -> BY_COST_ASCENDING;
            case NAME -> BY_NAME;
        };
    }

    @Override
    protected void init() {
        groupLists.clear();
        int listWidth = Math.min(MAX_LIST_WIDTH, (width - 40 - COLUMN_GAP) / 2);
        int totalHeight = Math.max(60, height - 72 - LIST_TOP);
        int center = width / 2;
        int leftX = center - COLUMN_GAP / 2 - listWidth;
        int rightX = center + COLUMN_GAP / 2;

        int count = groups.size();
        paged = count * MIN_GROUP_HEIGHT + (count - 1) * LABEL_SPACE > totalHeight;
        if (paged) {
            page = Math.min(page, count - 1);
            groupLists.add(addRenderableWidget(new EntryListWidget<>(leftX, LIST_TOP, listWidth, totalHeight,
                    groups.get(page).label(), this, EntryListWidget.Side.AVAILABLE)));
            addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1))
                    .bounds(leftX, LIST_TOP - 13, 12, 12).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1))
                    .bounds(leftX + listWidth - 12, LIST_TOP - 13, 12, 12).build());
        } else {
            int each = (totalHeight - (count - 1) * LABEL_SPACE) / count;
            int y = LIST_TOP;
            for (int i = 0; i < count; i++) {
                int height = i == count - 1 ? LIST_TOP + totalHeight - y : each;
                groupLists.add(addRenderableWidget(new EntryListWidget<>(leftX, y, listWidth, height,
                        groups.get(i).label(), this, EntryListWidget.Side.AVAILABLE)));
                y += height + LABEL_SPACE;
            }
        }
        chosenList = addRenderableWidget(new EntryListWidget<>(rightX, LIST_TOP, listWidth, totalHeight,
                Component.translatable("facets.screen.chosen"), this, EntryListWidget.Side.CHOSEN));

        int middle = LIST_TOP + totalHeight / 2;
        addButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> addSelected())
                .bounds(center - 10, middle - 22, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("facets.screen.add")))
                .build());
        removeButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> removeSelected())
                .bounds(center - 10, middle + 2, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("facets.screen.remove")))
                .build());

        int buttonY = height - 28;
        boolean back = context.hasOccupations();
        int buttonsLeft = center - (back ? 154 : 104);
        if (back) {
            addRenderableWidget(Button.builder(Component.translatable("facets.screen.back"), b -> back())
                    .bounds(buttonsLeft, buttonY, 100, 20).build());
            buttonsLeft += 104;
        }
        clearButton = addRenderableWidget(Button.builder(Component.translatable("facets.screen.clear"), b -> {
            chosen.clear();
            selected = null;
            serverError = null;
            updateLists();
        }).bounds(buttonsLeft, buttonY, 100, 20).build());
        confirmButton = addRenderableWidget(Button.builder(Component.translatable("facets.screen.confirm"), b -> confirm())
                .bounds(buttonsLeft + 104, buttonY, 100, 20).build());

        updateLists();
    }

    private void turnPage(int direction) {
        page = Math.floorMod(page + direction, groups.size());
        EntryListWidget<TraitEntry> list = groupLists.get(0);
        list.setMessage(groups.get(page).label());
        list.resetScroll();
        updateLists();
    }

    private void updateLists() {
        for (int i = 0; i < groupLists.size(); i++) {
            Group group = groups.get(paged ? page : i);
            groupLists.get(i).setEntries(group.entries().stream().filter(e -> !chosen.contains(e.id())).toList());
        }
        List<TraitEntry> build = new ArrayList<>(free);
        chosen.stream().map(byId::get).filter(Objects::nonNull).forEach(build::add);
        chosenList.setEntries(build);

        if (selected != null && chosen.contains(selected.id()) != (selectedSide == EntryListWidget.Side.CHOSEN)) {
            selected = null;
        }

        validationError = rules.validate(chosen, occupationId, context).orElse(null);

        addButton.active = !waiting && selected != null && selectedSide == EntryListWidget.Side.AVAILABLE && canAdd(selected);
        removeButton.active = !waiting && selected != null && selectedSide == EntryListWidget.Side.CHOSEN && !isFree(selected);
        confirmButton.active = !waiting && validationError == null;
        clearButton.active = !waiting;
    }

    private boolean isFree(TraitEntry entry) {
        return free.contains(entry);
    }

    private void addSelected() {
        if (selected != null && selectedSide == EntryListWidget.Side.AVAILABLE && canAdd(selected)) {
            chosen.add(selected.id());
            changed();
        }
    }

    private void removeSelected() {
        if (selected != null && selectedSide == EntryListWidget.Side.CHOSEN && !isFree(selected)) {
            chosen.remove(selected.id());
            changed();
        }
    }

    private void changed() {
        selected = null;
        serverError = null;
        updateLists();
    }

    private void back() {
        Objects.requireNonNull(minecraft).setScreen(new OccupationScreen(rules, context, occupationId, List.copyOf(chosen), granted));
    }

    private void confirm() {
        waiting = true;
        serverError = null;
        updateLists();
        PacketDistributor.sendToServer(new SubmitSelectionPayload(Optional.ofNullable(occupationId), List.copyOf(chosen)));
    }

    void onAccepted() {
        Objects.requireNonNull(minecraft).setScreen(null);
    }

    void onRejected(Component reason) {
        waiting = false;
        serverError = reason;
        updateLists();
    }

    @Override
    public void onEntryClicked(TraitEntry entry, EntryListWidget.Side side, boolean doubleClick) {
        if (waiting) {
            return;
        }
        selected = entry;
        selectedSide = side;
        if (doubleClick) {
            if (side == EntryListWidget.Side.AVAILABLE) {
                addSelected();
            } else {
                removeSelected();
            }
        }
        updateLists();
    }

    @Override
    public boolean isSelected(TraitEntry entry) {
        return entry == selected;
    }

    @Override
    public boolean isEnabled(TraitEntry entry, EntryListWidget.Side side) {
        return side == EntryListWidget.Side.AVAILABLE ? canAdd(entry) : !isFree(entry);
    }

    private boolean canAdd(TraitEntry entry) {
        return blockReason(entry) == null;
    }

    /** Everything the player would hold besides this trait: picked, free and granted traits. */
    private List<TraitHolder> held() {
        List<TraitHolder> held = new ArrayList<>();
        chosen.stream().map(byId::get).filter(Objects::nonNull).forEach(e -> held.add(e.holder()));
        free.forEach(e -> held.add(e.holder()));
        held.addAll(context.granted());
        return held;
    }

    /** Why a trait can't be added right now, or null if it can. */
    @Nullable
    private Component blockReason(TraitEntry entry) {
        List<TraitHolder> held = held();
        for (TraitHolder other : held) {
            if (other != entry.holder() && Trait.conflicts(entry.holder(), other)) {
                return Component.translatable("facets.screen.conflicts", other.value().name());
            }
        }
        for (TraitRef required : entry.trait().requires()) {
            if (held.stream().noneMatch(required::matches)) {
                return Component.translatable("facets.screen.requires", context.refName(required));
            }
        }
        if (rules.mode() == SelectionMode.COUNT && chosen.size() >= rules.maxTraits()) {
            return Component.translatable("facets.screen.limit");
        }
        Optional<ResourceLocation> categoryId = entry.trait().category();
        if (categoryId.isPresent()) {
            TraitCategory category = context.categories().get(categoryId.get());
            if (category != null && category.max().isPresent()) {
                long inCategory = chosen.stream().map(byId::get).filter(Objects::nonNull)
                        .filter(e -> e.trait().category().equals(categoryId)).count();
                if (inCategory >= category.max().get()) {
                    return Component.translatable("facets.screen.category_limit", category.name());
                }
            }
        }
        return null;
    }

    /**
     * Green "-4" for traits that spend points, red "+3" for traits that give points back, or the
     * category's color when it has one. Free traits from the occupation show "Free".
     */
    @Override
    @Nullable
    public Component costLabel(TraitEntry entry) {
        if (isFree(entry)) {
            return Component.translatable("facets.screen.free").withStyle(ChatFormatting.GRAY);
        }
        int cost = entry.trait().cost();
        if (rules.mode() != SelectionMode.POINTS || cost == 0) {
            return null;
        }
        Component label = Component.literal(cost > 0 ? "-" + cost : "+" + -cost);
        TextColor color = groupColors.get(entry.id());
        if (color != null) {
            return label.copy().withStyle(style -> style.withColor(color));
        }
        int themed = FacetsTheme.current().color(cost > 0 ? FacetsTheme.COST_POSITIVE : FacetsTheme.COST_NEGATIVE);
        return label.copy().withStyle(style -> style.withColor(themed & 0xFFFFFF));
    }

    private Component statusLine() {
        return switch (rules.mode()) {
            case POINTS -> {
                int remaining = rules.remainingPoints(occupation,
                        chosen.stream().map(byId::get).filter(Objects::nonNull).map(TraitEntry::trait).toList());
                yield Component.translatable("facets.screen.points",
                        Component.literal(String.valueOf(remaining)).withStyle(style -> style.withColor(
                                FacetsTheme.current().color(remaining < 0 ? FacetsTheme.POINTS_OVER : FacetsTheme.POINTS_LEFT) & 0xFFFFFF)));
            }
            case COUNT -> Component.translatable("facets.screen.count", chosen.size(), rules.maxTraits(), rules.minTraits());
            default -> Component.translatable("facets.screen.count_free", chosen.size());
        };
    }

    private int labelColor(int groupIndex) {
        TextColor color = groups.get(groupIndex).color();
        return color != null ? 0xFF000000 | color.getValue() : FacetsTheme.current().color(FacetsTheme.LABEL);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 14, FacetsTheme.current().color(FacetsTheme.TITLE));
        if (occupation != null) {
            graphics.drawCenteredString(font, Component.translatable("facets.screen.occupation", occupation.name()),
                    width / 2, 26, FacetsTheme.current().color(FacetsTheme.LABEL));
        }

        if (paged) {
            EntryListWidget<TraitEntry> list = groupLists.get(0);
            Component label = Component.translatable("facets.screen.page", groups.get(page).label(), page + 1, groups.size());
            graphics.drawCenteredString(font, label, list.getX() + list.getWidth() / 2, LIST_TOP - 11, labelColor(page));
        } else {
            for (int i = 0; i < groupLists.size(); i++) {
                EntryListWidget<TraitEntry> list = groupLists.get(i);
                graphics.drawString(font, list.getMessage(), list.getX(), list.getY() - 11, labelColor(i));
            }
        }
        graphics.drawString(font, chosenList.getMessage(), chosenList.getX(), LIST_TOP - 11, FacetsTheme.current().color(FacetsTheme.LABEL));

        int statusY = chosenList.getY() + chosenList.getHeight() + 8;
        graphics.drawCenteredString(font, statusLine(), width / 2, statusY, FacetsTheme.current().color(FacetsTheme.TEXT));
        Component error = serverError != null ? serverError : validationError;
        if (error != null) {
            graphics.drawCenteredString(font, error, width / 2, statusY + 12, FacetsTheme.current().color(FacetsTheme.ERROR));
        }

        List<EntryListWidget<TraitEntry>> lists = new ArrayList<>(groupLists);
        lists.add(chosenList);
        for (EntryListWidget<TraitEntry> list : lists) {
            TraitEntry hovered = list.hovered();
            if (hovered != null) {
                Component reason = list.side() == EntryListWidget.Side.AVAILABLE ? blockReason(hovered) : null;
                graphics.renderTooltip(font, TraitTooltips.build(font, hovered, context, costLabel(hovered), reason), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Opens the first step of the selection: the occupation list when there are occupations, the traits otherwise. */
    static Screen start(SelectionRules rules, SelectionContext context, @Nullable ResourceLocation occupation,
                        List<ResourceLocation> preselected, List<ResourceLocation> granted) {
        if (context.hasOccupations()) {
            return new OccupationScreen(rules, context, occupation, preselected, granted);
        }
        return new TraitSelectionScreen(rules, context, null, preselected, granted);
    }
}
