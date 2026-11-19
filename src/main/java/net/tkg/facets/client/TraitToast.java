package net.tkg.facets.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** "Trait gained / Trait lost" toast with the trait's icon, in the style of advancement toasts. */
public class TraitToast implements Toast {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("toast/advancement");
    private static final long DISPLAY_TIME = 5000L;
    private static final int GAINED_COLOR = 0xFF55FF55;
    private static final int LOST_COLOR = 0xFFFF5555;

    private final Component name;
    private final ItemStack icon;
    private final boolean gained;

    public TraitToast(Component name, ItemStack icon, boolean gained) {
        this.name = name;
        this.icon = icon;
        this.gained = gained;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long timeSinceLastVisible) {
        Font font = toasts.getMinecraft().font;
        graphics.blitSprite(BACKGROUND, 0, 0, width(), height());
        Component title = Component.translatable(gained ? "facets.toast.gained" : "facets.toast.lost");
        graphics.drawString(font, title, 30, 7, gained ? GAINED_COLOR : LOST_COLOR, false);
        FormattedCharSequence line = font.split(name, width() - 36).stream().findFirst().orElse(FormattedCharSequence.EMPTY);
        graphics.drawString(font, line, 30, 18, 0xFFFFFFFF, false);
        graphics.renderFakeItem(icon, 8, 8);
        return timeSinceLastVisible >= DISPLAY_TIME * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
    }
}
