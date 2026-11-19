package net.tkg.facets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Small button in the inventory's top left screen corner that opens My Traits, like the sidebar
 * buttons other mods put there. JEI and EMI are told about its area so their panels stay clear of it.
 */
public class TraitsButton extends AbstractButton {
    public static final int SIZE = 20;
    private static final ItemStack ICON = new ItemStack(Items.WRITABLE_BOOK);

    private final Screen screen;

    public TraitsButton(Screen screen) {
        super(x(), y(), SIZE, SIZE, Component.translatable("facets.screen.overview"));
        this.screen = screen;
        setTooltip(Tooltip.create(Component.translatable("facets.screen.overview")));
    }

    private static int x() {
        return FacetsClientConfig.INVENTORY_BUTTON_X.get();
    }

    private static int y() {
        return FacetsClientConfig.INVENTORY_BUTTON_Y.get();
    }

    /** The area the button takes on the given screen, or null if it isn't shown there. */
    @Nullable
    public static Rect2i area(Screen screen) {
        if (!FacetsClient.showsTraitsButton(screen)) {
            return null;
        }
        return new Rect2i(x(), y(), SIZE, SIZE);
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().setScreen(new TraitOverviewScreen(screen));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        setX(x());
        setY(y());
        setMessage(Component.empty());
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        setMessage(Component.translatable("facets.screen.overview"));
        graphics.renderItem(ICON, getX() + 2, getY() + 2);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
