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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Scrollable list of traits, one per row. Click selects a row, double-click moves it to the other side. */
public class TraitListWidget extends AbstractWidget {
    public enum Side {
        AVAILABLE,
        CHOSEN
    }

    private static final int ROW_HEIGHT = 20;
    private static final int PADDING = 2;
    private static final long DOUBLE_CLICK_MS = 250;

    private final TraitSelectionScreen screen;
    private final Side side;
    private List<TraitEntry> entries = List.of();
    private double scroll;
    @Nullable
    private TraitEntry hovered;
    @Nullable
    private TraitEntry lastClicked;
    private long lastClickTime;

    public TraitListWidget(int x, int y, int width, int height, Component title, TraitSelectionScreen screen, Side side) {
        super(x, y, width, height, title);
        this.screen = screen;
        this.side = side;
    }

    public void setEntries(List<TraitEntry> entries) {
        this.entries = entries;
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    @Nullable
    public TraitEntry hovered() {
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

        graphics.fill(x, y, x + width, y + height, 0xA0000000);
        graphics.renderOutline(x, y, width, height, 0xFF555555);

        if (entries.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("facets.screen.empty"), x + width / 2, y + height / 2 - 4, 0x808080);
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
            TraitEntry entry = entries.get(i);
            boolean enabled = side == Side.CHOSEN || screen.canAdd(entry);
            boolean rowHovered = mouseInside && mouseX >= rowLeft && mouseX < rowRight && mouseY >= top && mouseY < top + ROW_HEIGHT;

            if (screen.isSelected(entry)) {
                graphics.fill(rowLeft, top, rowRight, top + ROW_HEIGHT, 0x50FFFFFF);
                graphics.renderOutline(rowLeft, top, rowRight - rowLeft, ROW_HEIGHT, 0xFFFFFFFF);
            } else if (rowHovered) {
                graphics.fill(rowLeft, top, rowRight, top + ROW_HEIGHT, enabled ? 0x30FFFFFF : 0x18FFFFFF);
            }
            if (rowHovered) {
                hovered = entry;
            }

            graphics.renderItem(entry.icon(), rowLeft + 2, top + 2);

            int textLeft = rowLeft + 22;
            int textRight = rowRight - 3;
            Component cost = screen.costLabel(entry);
            if (cost != null) {
                int costWidth = font.width(cost);
                graphics.drawString(font, cost, textRight - costWidth, top + 6, 0xFFFFFF);
                textRight -= costWidth + 4;
            }
            FormattedCharSequence name = Language.getInstance()
                    .getVisualOrder(font.substrByWidth(entry.trait().name(), Math.max(0, textRight - textLeft)));
            graphics.drawString(font, name, textLeft, top + 6, enabled ? 0xFFFFFF : 0x707070);
        }
        graphics.disableScissor();

        if (scrollbar) {
            int trackTop = y + 1;
            int trackHeight = height - 2;
            int contentHeight = entries.size() * ROW_HEIGHT + PADDING * 2;
            int barHeight = Math.max(12, trackHeight * trackHeight / contentHeight);
            int barTop = trackTop + (int) (scroll * (trackHeight - barHeight) / maxScroll());
            int barLeft = x + width - 5;
            graphics.fill(barLeft, trackTop, barLeft + 4, trackTop + trackHeight, 0xFF202020);
            graphics.fill(barLeft, barTop, barLeft + 4, barTop + barHeight, 0xFF909090);
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
            TraitEntry entry = entries.get(index);
            long now = Util.getMillis();
            boolean doubleClick = entry == lastClicked && now - lastClickTime < DOUBLE_CLICK_MS;
            lastClicked = doubleClick ? null : entry;
            lastClickTime = now;
            playDownSound(Minecraft.getInstance().getSoundManager());
            screen.onEntryClicked(entry, side, doubleClick);
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
