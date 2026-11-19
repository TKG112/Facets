package net.tkg.facets.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.tkg.facets.trait.TraitHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.tkg.facets.client.theme.FacetsTheme;
import net.tkg.facets.trait.Keyed;
import net.tkg.facets.trait.Occupation;
import net.tkg.facets.trait.SelectionContext;
import net.tkg.facets.trait.SelectionMode;
import net.tkg.facets.trait.SelectionRules;
import net.tkg.facets.trait.Trait;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** First step of the selection when the pack defines occupations. */
public class OccupationScreen extends Screen implements EntryListWidget.Host<OccupationEntry> {
    private static final int LIST_TOP = 44;
    private static final int MAX_LIST_WIDTH = 200;
    private static final int COLUMN_GAP = 12;

    private final SelectionRules rules;
    private final SelectionContext context;
    private final List<ResourceLocation> traits;
    private final List<ResourceLocation> granted;
    private final List<OccupationEntry> entries = new ArrayList<>();

    private EntryListWidget<OccupationEntry> list;
    private Button nextButton;
    @Nullable
    private OccupationEntry selected;
    private int detailsX;
    private int detailsWidth;
    private int detailsHeight;

    /**
     * @param occupation occupation to preselect
     * @param traits     traits picked so far, handed on to the trait screen
     */
    public OccupationScreen(SelectionRules rules, SelectionContext context, @Nullable ResourceLocation occupation,
                            List<ResourceLocation> traits, List<ResourceLocation> granted) {
        super(Component.translatable("facets.screen.occupation_title"));
        this.rules = rules;
        this.context = context;
        this.traits = traits;
        this.granted = granted;
        for (Keyed<Occupation> holder : context.allOccupations()) {
            OccupationEntry entry = new OccupationEntry(holder);
            entries.add(entry);
            if (entry.id().equals(occupation)) {
                selected = entry;
            }
        }
    }

    @Override
    protected void init() {
        int listWidth = Math.min(MAX_LIST_WIDTH, (width - 40 - COLUMN_GAP) / 2);
        int totalHeight = Math.max(60, height - 72 - LIST_TOP);
        int center = width / 2;

        list = addRenderableWidget(new EntryListWidget<>(center - COLUMN_GAP / 2 - listWidth, LIST_TOP, listWidth, totalHeight,
                Component.translatable("facets.screen.occupations"), this, EntryListWidget.Side.AVAILABLE));
        list.setEntries(entries);
        detailsX = center + COLUMN_GAP / 2;
        detailsWidth = listWidth;
        detailsHeight = totalHeight;

        nextButton = addRenderableWidget(Button.builder(Component.translatable("facets.screen.next"), b -> next())
                .bounds(center - 50, height - 28, 100, 20).build());
        nextButton.active = selected != null;
    }

    private void next() {
        if (selected == null) {
            return;
        }
        Objects.requireNonNull(minecraft).setScreen(new TraitSelectionScreen(rules, context, selected.id(), traits, granted));
    }

    @Override
    public boolean isSelected(OccupationEntry entry) {
        return entry == selected;
    }

    @Override
    public boolean isEnabled(OccupationEntry entry, EntryListWidget.Side side) {
        return true;
    }

    @Override
    @Nullable
    public Component costLabel(OccupationEntry entry) {
        return pointsLabel(entry.occupation());
    }

    @Nullable
    private Component pointsLabel(Occupation occupation) {
        int points = occupation.points();
        if (rules.mode() != SelectionMode.POINTS || points == 0) {
            return null;
        }
        return points > 0
                ? Component.literal("+" + points).withStyle(style -> style.withColor(FacetsTheme.current().color(FacetsTheme.POINTS_LEFT) & 0xFFFFFF))
                : Component.literal(String.valueOf(points)).withStyle(style -> style.withColor(FacetsTheme.current().color(FacetsTheme.POINTS_OVER) & 0xFFFFFF));
    }

    @Override
    public void onEntryClicked(OccupationEntry entry, EntryListWidget.Side side, boolean doubleClick) {
        selected = entry;
        nextButton.active = true;
        if (doubleClick) {
            next();
        }
    }

    private void renderDetails(GuiGraphics graphics) {
        int x = detailsX;
        int y = LIST_TOP;
        FacetsTheme theme = FacetsTheme.current();
        if (theme.sprite(FacetsTheme.SPRITE_PANEL) != null) {
            theme.fill(graphics, FacetsTheme.SPRITE_PANEL, FacetsTheme.PANEL_BACKGROUND, x, y, detailsWidth, detailsHeight);
        } else {
            graphics.fill(x, y, x + detailsWidth, y + detailsHeight, theme.color(FacetsTheme.PANEL_BACKGROUND));
            graphics.renderOutline(x, y, detailsWidth, detailsHeight, theme.color(FacetsTheme.PANEL_BORDER));
        }
        if (selected == null) {
            graphics.drawCenteredString(font, Component.translatable("facets.screen.pick_occupation"),
                    x + detailsWidth / 2, y + detailsHeight / 2 - 4, theme.color(FacetsTheme.TEXT_EMPTY));
            return;
        }

        Occupation occupation = selected.occupation();
        int textWidth = detailsWidth - 12;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(occupation.name().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD).getVisualOrderText());
        lines.add(FormattedCharSequence.EMPTY);
        if (!occupation.description().getString().isEmpty()) {
            lines.addAll(font.split(occupation.description().copy().withStyle(ChatFormatting.GRAY), textWidth));
            lines.add(FormattedCharSequence.EMPTY);
        }
        Component points = pointsLabel(occupation);
        if (points != null) {
            lines.add(Component.translatable("facets.screen.occupation_points", points).getVisualOrderText());
        }
        List<TraitHolder> free = context.freeTraits(occupation);
        if (!free.isEmpty()) {
            Component names = ComponentUtils.formatList(free, trait -> trait.value().name());
            lines.addAll(font.split(Component.translatable("facets.screen.occupation_traits", names).withStyle(ChatFormatting.GRAY), textWidth));
        }

        graphics.enableScissor(x + 1, y + 1, x + detailsWidth - 1, y + detailsHeight - 1);
        int lineY = y + 6;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x + 6, lineY, theme.color(FacetsTheme.TEXT));
            lineY += 10;
        }
        graphics.disableScissor();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, FacetsTheme.current().color(FacetsTheme.TITLE));
        graphics.drawString(font, list.getMessage(), list.getX(), LIST_TOP - 11, FacetsTheme.current().color(FacetsTheme.LABEL));
        renderDetails(graphics);
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
