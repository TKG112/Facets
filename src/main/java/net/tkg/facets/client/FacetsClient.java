package net.tkg.facets.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tkg.facets.Facets;
import net.tkg.facets.network.OpenSelectionPayload;
import net.tkg.facets.network.SelectionResultPayload;

@EventBusSubscriber(modid = Facets.MODID, value = Dist.CLIENT)
public final class FacetsClient {
    /**
     * The open request is held until the player is in the world with no other screen up. Opening it
     * right away would get closed by the level loading screen on join, or by the death screen.
     */
    private static OpenSelectionPayload pending;

    private FacetsClient() {
    }

    public static void openSelection(OpenSelectionPayload payload) {
        pending = payload;
    }

    public static void handleResult(SelectionResultPayload payload) {
        if (Minecraft.getInstance().screen instanceof TraitSelectionScreen screen) {
            if (payload.accepted()) {
                screen.onAccepted();
            } else {
                screen.onRejected(payload.message());
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pending == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (minecraft.screen == null || minecraft.screen instanceof TraitSelectionScreen) {
            OpenSelectionPayload payload = pending;
            pending = null;
            minecraft.setScreen(new TraitSelectionScreen(payload.rules(), payload.current()));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        pending = null;
    }
}
