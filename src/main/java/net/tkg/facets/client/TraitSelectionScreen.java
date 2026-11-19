package net.tkg.facets.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tkg.facets.network.SubmitSelectionPayload;
import net.tkg.facets.registry.FacetsRegistries;
import net.tkg.facets.trait.SelectionMode;
import net.tkg.facets.trait.SelectionRules;
import net.tkg.facets.trait.Trait;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TraitSelectionScreen extends Screen {
    private static final int LIST_TOP = 44;
    private static final int LABEL_SPACE = 14;
    private static final int MAX_LIST_WIDTH = 200;
    private static final int COLUMN_GAP = 32;
    private static final int TOOLTIP_WIDTH = 200;

    /**
     * Highest cost first. With positive traits on top and negative traits below, the biggest values
     * end up at the outer edges and the cheapest ones meet in the middle.
     */
    private static final Comparator<TraitEntry> BY_COST = Comparator
            .comparingInt((TraitEntry e) -> -e.trait().cost())
            .thenComparing(e -> e.trait().name().getString());

    private final SelectionRules rules;
    private final Map<ResourceLocation, TraitEntry> byId = new HashMap<>();
    /** Traits with a cost of 0 or more. Holds every trait when there are no negative ones. */
    private final List<TraitEntry> positive = new ArrayList<>();
    private final List<TraitEntry> negative = new ArrayList<>();
    private final List<ResourceLocation> chosen = new ArrayList<>();

    private TraitListWidget positiveList;
    @Nullable
    private TraitListWidget negativeList;
    private TraitListWidget chosenList;
    private Button addButton;
    private Button removeButton;
    private Button clearButton;
    private Button confirmButton;

    @Nullable
    private TraitEntry selected;
    private TraitListWidget.Side selectedSide = TraitListWidget.Side.AVAILABLE;
    @Nullable
    private Component validationError;
    @Nullable
    private Component serverError;
    private boolean waiting;

    public TraitSelectionScreen(SelectionRules rules, List<ResourceLocation> preselected) {
        super(Component.translatable("facets.screen.title"));
        this.rules = rules;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.registryAccess().registry(FacetsRegistries.TRAIT).ifPresent(registry ->
                    registry.entrySet().forEach(e -> {
                        if (e.getValue().selectable()) {
                            TraitEntry entry = new TraitEntry(e.getKey().location(), e.getValue());
                            byId.put(entry.id(), entry);
                            (entry.trait().cost() < 0 ? negative : positive).add(entry);
                        }
                    }));
        }
        positive.sort(BY_COST);
        negative.sort(BY_COST);

        for (ResourceLocation id : preselected) {
            if (byId.containsKey(id) && !chosen.contains(id)) {
                chosen.add(id);
            }
        }
    }

    private boolean split() {
        return !negative.isEmpty();
    }

    @Override
    protected void init() {
        int listWidth = Math.min(MAX_LIST_WIDTH, (width - 40 - COLUMN_GAP) / 2);
        int totalHeight = Math.max(60, height - 72 - LIST_TOP);
        int center = width / 2;
        int leftX = center - COLUMN_GAP / 2 - listWidth;
        int rightX = center + COLUMN_GAP / 2;

        if (split()) {
            int topHeight = (totalHeight - LABEL_SPACE) / 2;
            positiveList = addRenderableWidget(new TraitListWidget(leftX, LIST_TOP, listWidth, topHeight,
                    Component.translatable("facets.screen.positive"), this, TraitListWidget.Side.AVAILABLE));
            negativeList = addRenderableWidget(new TraitListWidget(leftX, LIST_TOP + topHeight + LABEL_SPACE, listWidth,
                    totalHeight - topHeight - LABEL_SPACE,
                    Component.translatable("facets.screen.negative"), this, TraitListWidget.Side.AVAILABLE));
        } else {
            positiveList = addRenderableWidget(new TraitListWidget(leftX, LIST_TOP, listWidth, totalHeight,
                    Component.translatable("facets.screen.available"), this, TraitListWidget.Side.AVAILABLE));
            negativeList = null;
        }
        chosenList = addRenderableWidget(new TraitListWidget(rightX, LIST_TOP, listWidth, totalHeight,
                Component.translatable("facets.screen.chosen"), this, TraitListWidget.Side.CHOSEN));

        int middle = LIST_TOP + totalHeight / 2;
        addButton = addRenderableWidget(Button.builder(Component.literal(">"), b -> addSelected())
                .bounds(center - 10, middle - 22, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("facets.screen.add")))
                .build());
        removeButton = addRenderableWidget(Button.builder(Component.literal("<"), b -> removeSelected())
                .bounds(center - 10, middle + 2, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("facets.screen.remove")))
                .build());

        clearButton = addRenderableWidget(Button.builder(Component.translatable("facets.screen.clear"), b -> {
            chosen.clear();
            selected = null;
            serverError = null;
            updateLists();
        }).bounds(center - 104, height - 28, 100, 20).build());
        confirmButton = addRenderableWidget(Button.builder(Component.translatable("facets.screen.confirm"), b -> confirm())
                .bounds(center + 4, height - 28, 100, 20).build());

        updateLists();
    }

    private void updateLists() {
        positiveList.setEntries(positive.stream().filter(e -> !chosen.contains(e.id())).toList());
        if (negativeList != null) {
            negativeList.setEntries(negative.stream().filter(e -> !chosen.contains(e.id())).toList());
        }
        chosenList.setEntries(chosen.stream().map(byId::get).filter(Objects::nonNull).toList());

        if (selected != null && chosen.contains(selected.id()) != (selectedSide == TraitListWidget.Side.CHOSEN)) {
            selected = null;
        }

        validationError = rules.validate(chosen, id -> {
            TraitEntry entry = byId.get(id);
            return entry == null ? null : entry.trait();
        }).orElse(null);

        addButton.active = !waiting && selected != null && selectedSide == TraitListWidget.Side.AVAILABLE && canAdd(selected);
        removeButton.active = !waiting && selected != null && selectedSide == TraitListWidget.Side.CHOSEN;
        confirmButton.active = !waiting && validationError == null;
        clearButton.active = !waiting;
    }

    private void addSelected() {
        if (selected != null && selectedSide == TraitListWidget.Side.AVAILABLE && canAdd(selected)) {
            chosen.add(selected.id());
            changed();
        }
    }

    private void removeSelected() {
        if (selected != null && selectedSide == TraitListWidget.Side.CHOSEN) {
            chosen.remove(selected.id());
            changed();
        }
    }

    private void changed() {
        selected = null;
        serverError = null;
        updateLists();
    }

    private void confirm() {
        waiting = true;
        serverError = null;
        updateLists();
        PacketDistributor.sendToServer(new SubmitSelectionPayload(List.copyOf(chosen)));
    }

    void onAccepted() {
        Objects.requireNonNull(minecraft).setScreen(null);
    }

    void onRejected(Component reason) {
        waiting = false;
        serverError = reason;
        updateLists();
    }

    void onEntryClicked(TraitEntry entry, TraitListWidget.Side side, boolean doubleClick) {
        if (waiting) {
            return;
        }
        selected = entry;
        selectedSide = side;
        if (doubleClick) {
            if (side == TraitListWidget.Side.AVAILABLE) {
                addSelected();
            } else {
                removeSelected();
            }
        }
        updateLists();
    }

    boolean isSelected(TraitEntry entry) {
        return entry == selected;
    }

    boolean canAdd(TraitEntry entry) {
        return blockReason(entry) == null;
    }

    /** Why a trait can't be added right now, or null if it can. */
    @Nullable
    private Component blockReason(TraitEntry entry) {
        if (rules.mode() == SelectionMode.COUNT && chosen.size() >= rules.maxTraits()) {
            return Component.translatable("facets.screen.limit");
        }
        for (ResourceLocation id : chosen) {
            TraitEntry other = byId.get(id);
            if (other != null && Trait.conflicts(entry.id(), entry.trait(), other.id(), other.trait())) {
                return Component.translatable("facets.screen.conflicts", other.trait().name());
            }
        }
        return null;
    }

    /** Green "-4" for positive traits that spend points, red "+3" for negative traits that give points back. */
    @Nullable
    Component costLabel(TraitEntry entry) {
        int cost = entry.trait().cost();
        if (rules.mode() != SelectionMode.POINTS || cost == 0) {
            return null;
        }
        return cost > 0
                ? Component.literal("-" + cost).withStyle(ChatFormatting.GREEN)
                : Component.literal("+" + -cost).withStyle(ChatFormatting.RED);
    }

    private Component statusLine() {
        return switch (rules.mode()) {
            case POINTS -> {
                int remaining = rules.remainingPoints(chosen.stream().map(byId::get).filter(Objects::nonNull).map(TraitEntry::trait).toList());
                yield Component.translatable("facets.screen.points",
                        Component.literal(String.valueOf(remaining)).withStyle(remaining < 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
            }
            case COUNT -> Component.translatable("facets.screen.count", chosen.size(), rules.maxTraits(), rules.minTraits());
            default -> Component.translatable("facets.screen.count_free", chosen.size());
        };
    }

    private List<FormattedCharSequence> tooltip(TraitEntry entry, TraitListWidget.Side side) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(entry.trait().name().copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());
        Component description = entry.trait().description();
        if (!description.getString().isEmpty()) {
            lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), TOOLTIP_WIDTH));
        }
        Component cost = costLabel(entry);
        if (cost != null) {
            lines.add(Component.translatable("facets.screen.cost", cost).withStyle(ChatFormatting.GRAY).getVisualOrderText());
        }
        if (side == TraitListWidget.Side.AVAILABLE) {
            Component reason = blockReason(entry);
            if (reason != null) {
                lines.addAll(font.split(reason.copy().withStyle(ChatFormatting.RED), TOOLTIP_WIDTH));
            }
        }
        return lines;
    }

    private void drawLabel(GuiGraphics graphics, @Nullable TraitListWidget list) {
        if (list != null) {
            graphics.drawString(font, list.getMessage(), list.getX(), list.getY() - 11, 0xA0A0A0);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        drawLabel(graphics, positiveList);
        drawLabel(graphics, negativeList);
        drawLabel(graphics, chosenList);

        int statusY = chosenList.getY() + chosenList.getHeight() + 8;
        graphics.drawCenteredString(font, statusLine(), width / 2, statusY, 0xFFFFFF);
        Component error = serverError != null ? serverError : validationError;
        if (error != null) {
            graphics.drawCenteredString(font, error, width / 2, statusY + 12, 0xFF5555);
        }

        for (TraitListWidget list : new TraitListWidget[]{positiveList, negativeList, chosenList}) {
            if (list != null && list.hovered() != null) {
                graphics.renderTooltip(font, tooltip(list.hovered(), list.side()), mouseX, mouseY);
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
}
