package net.tkg.facets.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.tkg.facets.client.theme.FacetsTheme;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Scrollable list with an icon and a name per row. Click selects a row, double-click activates it. */
public class EntryListWidget<E extends EntryListWidget.Entry> extends AbstractWidget {
    public enum Side {
        AVAILABLE,
        CHOSEN
    }

    public interface Entry {
        Component name();

        ItemStack icon();
    }

    /** The screen a list lives on. */
    public interface Host<E> {
        boolean isSelected(E entry);

        /** Disabled entries are drawn grey. */
        boolean isEnabled(E entry, Side side);

        /** Small label drawn on the right of the row, e.g. a trait's cost. */
        @Nullable
        Component costLabel(E entry);

        void onEntryClicked(E entry, Side side, boolean doubleClick);
    }

    private static final int ROW_HEIGHT = 20;
    private static final int PADDING = 2;
    private static final long DOUBLE_CLICK_MS = 250;

    private final Host<E> host;
    private final Side side;
    private List<E> entries = List.of();
    private double scroll;
    @Nullable
    private E hovered;
    @Nullable
    private E lastClicked;
    private long lastClickTime;

    public EntryListWidget(int x, int y, int width, int height, Component title, Host<E> host, Side side) {
        super(x, y, width, height, title);
        this.host = host;
        this.side = side;
    }

    public void setEntries(List<E> entries) {
        if (!entries.equals(this.entries)) {
            this.entries = entries;
            scroll = Mth.clamp(scroll, 0, maxScroll());
        }
    }

    public void resetScroll() {
        scroll = 0;
    }

    @Nullable
    public E hovered() {
        return hovered;
    }

    public Side side() {
        return side;
    }

    private int maxScroll() {
        return Math.max(0, entries.size() * ROW_HEIGHT - (height - PADDING * 2));
    }

    private int rowTop(int index) {
        return getY() + PADDING + index * ROW_HEIGHT - (int) scroll;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = getX();
        int y = getY();
        hovered = null;
        FacetsTheme theme = FacetsTheme.current();

        if (theme.sprite(FacetsTheme.SPRITE_PANEL) != null) {
            theme.fill(graphics, FacetsTheme.SPRITE_PANEL, FacetsTheme.PANEL_BACKGROUND, x, y, width, height);
        } else {
            graphics.fill(x, y, x + width, y + height, theme.color(FacetsTheme.PANEL_BACKGROUND));
            graphics.renderOutline(x, y, width, height, theme.color(FacetsTheme.PANEL_BORDER));
        }

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("facets.screen.empty"), x + width / 2, y + height / 2 - 4, theme.color(FacetsTheme.TEXT_EMPTY));
            return;
        }

        boolean scrollbar = maxScroll() > 0;
        int rowLeft = x + PADDING;
        int rowRight = x + width - PADDING - (scrollbar ? 6 : 0);
        boolean mouseInside = isMouseOver(mouseX, mouseY);

        graphics.enableScissor(x + 1, y + 1, x + width - 1, y + height - 1);
        for (int i = 0; i < entries.size(); i++) {
            int top = rowTop(i);
            if (top + ROW_HEIGHT < y || top > y + height) {
                continue;
            }
            E entry = entries.get(i);
            boolean enabled = host.isEnabled(entry, side);
            boolean rowHovered = mouseInside && mouseX >= rowLeft && mouseX < rowRight && mouseY >= top && mouseY < top + ROW_HEIGHT;

            if (host.isSelected(entry)) {
                if (theme.sprite(FacetsTheme.SPRITE_ROW_SELECTED) != null) {
                    theme.fill(graphics, FacetsTheme.SPRITE_ROW_SELECTED, FacetsTheme.ROW_SELECTED, rowLeft, top, rowRight - rowLeft, ROW_HEIGHT);
                } else {
                    graphics.fill(rowLeft, top, rowRight, top + ROW_HEIGHT, theme.color(FacetsTheme.ROW_SELECTED));
                    graphics.renderOutline(rowLeft, top, rowRight - rowLeft, ROW_HEIGHT, theme.color(FacetsTheme.ROW_SELECTED_BORDER));
                }
            } else if (rowHovered) {
                theme.fill(graphics, FacetsTheme.SPRITE_ROW_HOVER, enabled ? FacetsTheme.ROW_HOVER : FacetsTheme.ROW_HOVER_DISABLED,
                        rowLeft, top, rowRight - rowLeft, ROW_HEIGHT);
            }
            if (rowHovered) {
                hovered = entry;
            }

            graphics.renderItem(entry.icon(), rowLeft + 2, top + 2);

            int textLeft = rowLeft + 22;
            int textRight = rowRight - 3;
            Component cost = host.costLabel(entry);
            if (cost != null) {
                int costWidth = font.width(cost);
                graphics.drawString(font, cost, textRight - costWidth, top + 6, theme.color(FacetsTheme.TEXT));
                textRight -= costWidth + 4;
            }
            FormattedCharSequence name = Language.getInstance()
                    .getVisualOrder(font.substrByWidth(entry.name(), Math.max(0, textRight - textLeft)));
            graphics.drawString(font, name, textLeft, top + 6, theme.color(enabled ? FacetsTheme.TEXT : FacetsTheme.TEXT_DISABLED));
        }
        graphics.disableScissor();

        if (scrollbar) {
            int trackTop = y + 1;
            int trackHeight = height - 2;
            int contentHeight = entries.size() * ROW_HEIGHT + PADDING * 2;
            int barHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
            int barTop = trackTop + (int) (scroll * (trackHeight - barHeight) / maxScroll());
            int barLeft = x + width - 5;
            graphics.fill(barLeft, trackTop, barLeft + 4, trackTop + trackHeight, theme.color(FacetsTheme.SCROLLBAR_TRACK));
            graphics.fill(barLeft, barTop, barLeft + 4, barTop + barHeight, theme.color(FacetsTheme.SCROLLBAR));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        double offset = mouseY - getY() - PADDING + scroll;
        int index = (int) Math.floor(offset / ROW_HEIGHT);
        if (offset >= 0 && index < entries.size()) {
            E entry = entries.get(index);
            long now = Util.getMillis();
            boolean doubleClick = entry == lastClicked && now - lastClickTime < DOUBLE_CLICK_MS;
            lastClicked = doubleClick ? null : entry;
            lastClickTime = now;
            playDownSound(Minecraft.getInstance().getSoundManager());
            host.onEntryClicked(entry, side, doubleClick);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scroll = Mth.clamp(scroll - scrollY * ROW_HEIGHT, 0, maxScroll());
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }
}
