package net.tkg.facets.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.client.theme.FacetsTheme;
import net.tkg.facets.trait.Occupation;
import net.tkg.facets.trait.SelectionContext;
import net.tkg.facets.trait.TraitHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Read-only list of the player's current traits, opened with the "My Traits" key. */
public class TraitOverviewScreen extends Screen implements EntryListWidget.Host<TraitEntry> {
    private static final int LIST_TOP = 44;
    private static final int MAX_LIST_WIDTH = 220;

    private EntryListWidget<TraitEntry> list;
    @Nullable
    private SelectionContext context;
    @Nullable
    private Component occupation;
    @Nullable
    private TraitEntry selected;
    /** Screen to go back to when closing, e.g. the inventory. */
    @Nullable
    private final Screen parent;

    public TraitOverviewScreen() {
        this(null);
    }

    public TraitOverviewScreen(@Nullable Screen parent) {
        super(Component.translatable("facets.screen.overview"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        Objects.requireNonNull(minecraft).setScreen(parent);
    }

    @Override
    protected void init() {
        int listWidth = Math.min(MAX_LIST_WIDTH, width - 40);
        int listHeight = Math.max(40, height - 40 - LIST_TOP);
        list = addRenderableWidget(new EntryListWidget<>(width / 2 - listWidth / 2, LIST_TOP, listWidth, listHeight,
                Component.translatable("facets.screen.overview.count", 0), this, EntryListWidget.Side.CHOSEN));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 50, height - 28, 100, 20).build());
        refresh();
    }

    /** Rebuilds the list from the traits the server last sent. */
    public void refresh() {
        context = TraitTooltips.clientContext(List.of());
        List<TraitEntry> entries = new ArrayList<>();
        occupation = null;
        for (ResourceLocation id : FacetsClient.traits()) {
            TraitHolder holder = context.trait(id);
            if (holder != null) {
                entries.add(new TraitEntry(holder));
            }
        }
        Occupation current = context.occupation(FacetsClient.occupation().orElse(null));
        if (current != null) {
            occupation = Component.translatable("facets.screen.occupation", current.name());
        }
        list.setEntries(entries);
        list.setMessage(Component.translatable("facets.screen.overview.count", entries.size()));
    }

    @Override
    public boolean isSelected(TraitEntry entry) {
        return entry == selected;
    }

    @Override
    public boolean isEnabled(TraitEntry entry, EntryListWidget.Side side) {
        return true;
    }

    @Override
    @Nullable
    public Component costLabel(TraitEntry entry) {
        return null;
    }

    @Override
    public void onEntryClicked(TraitEntry entry, EntryListWidget.Side side, boolean doubleClick) {
        selected = entry;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, FacetsTheme.current().color(FacetsTheme.TITLE));
        if (occupation != null) {
            graphics.drawCenteredString(font, occupation, width / 2, 26, FacetsTheme.current().color(FacetsTheme.LABEL));
        }
        graphics.drawString(font, list.getMessage(), list.getX(), LIST_TOP - 11, FacetsTheme.current().color(FacetsTheme.LABEL));

        TraitEntry hovered = list.hovered();
        if (hovered != null && context != null) {
            graphics.renderTooltip(font, TraitTooltips.build(font, hovered, Objects.requireNonNull(context), null, null), mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // The same key closes the screen again
        if (FacetsKeys.OPEN_TRAITS.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
