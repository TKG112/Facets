package net.tkg.facets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tkg.facets.Facets;
import net.tkg.facets.data.TraitData;
import net.tkg.facets.network.OpenSelectionPayload;
import net.tkg.facets.network.SelectionResultPayload;
import net.tkg.facets.network.SyncDataPayload;
import net.tkg.facets.network.SyncTraitsPayload;
import net.tkg.facets.network.TraitNotifyPayload;
import net.tkg.facets.trait.SelectionContext;
import net.tkg.facets.trait.Trait;
import net.minecraft.world.item.ItemStack;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Facets.MODID, value = Dist.CLIENT)
public final class FacetsClient {
    /**
     * The open request is held until the player is in the world with no other screen up. Opening it
     * right away would get closed by the level loading screen on join, or by the death screen.
     */
    private static OpenSelectionPayload pending;
    private static List<ResourceLocation> traits = List.of();
    private static Optional<ResourceLocation> occupation = Optional.empty();
    private static TraitData data = TraitData.EMPTY;

    private FacetsClient() {
    }

    /** Every trait the local player holds, as last sent by the server. */
    public static List<ResourceLocation> traits() {
        return traits;
    }

    public static Optional<ResourceLocation> occupation() {
        return occupation;
    }

    /** Traits, categories and occupations as last sent by the server. */
    public static TraitData data() {
        return data;
    }

    public static void syncData(SyncDataPayload payload) {
        data = payload.data();
        if (Minecraft.getInstance().screen instanceof TraitOverviewScreen screen) {
            screen.refresh();
        }
    }

    public static void notifyTrait(TraitNotifyPayload payload) {
        Trait trait = data.trait(payload.trait());
        if (trait != null) {
            Minecraft.getInstance().getToasts().addToast(new TraitToast(trait.name(), new ItemStack(trait.icon()), payload.gained()));
        }
    }

    public static void openSelection(OpenSelectionPayload payload) {
        pending = payload;
    }

    private static void playConfirmSound() {
        ResourceLocation sound = ResourceLocation.tryParse(FacetsClientConfig.CONFIRM_SOUND.get());
        if (sound == null || FacetsClientConfig.CONFIRM_SOUND.get().isEmpty()) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(sound),
                FacetsClientConfig.CONFIRM_SOUND_PITCH.get().floatValue(), FacetsClientConfig.CONFIRM_SOUND_VOLUME.get().floatValue()));
    }

    /** Whether the My Traits button is added to this screen. */
    public static boolean showsTraitsButton(@org.jetbrains.annotations.Nullable net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof InventoryScreen && FacetsClientConfig.INVENTORY_BUTTON.get();
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (showsTraitsButton(event.getScreen())) {
            event.addListener(new TraitsButton(event.getScreen()));
        }
    }

    public static void syncTraits(SyncTraitsPayload payload) {
        traits = List.copyOf(payload.traits());
        occupation = payload.occupation();
        if (Minecraft.getInstance().screen instanceof TraitOverviewScreen screen) {
            screen.refresh();
        }
    }

    public static void handleResult(SelectionResultPayload payload) {
        if (Minecraft.getInstance().screen instanceof TraitSelectionScreen screen) {
            if (payload.accepted()) {
                playConfirmSound();
                screen.onAccepted();
            } else {
                screen.onRejected(payload.message());
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (pending != null && (minecraft.screen == null
                || minecraft.screen instanceof TraitSelectionScreen
                || minecraft.screen instanceof OccupationScreen
                || minecraft.screen instanceof TraitOverviewScreen)) {
            OpenSelectionPayload payload = pending;
            pending = null;
            SelectionContext context = TraitTooltips.clientContext(payload.granted());
            minecraft.setScreen(TraitSelectionScreen.start(payload.rules(), context,
                    payload.occupation().orElse(null), payload.current(), payload.granted()));
        }

        while (FacetsKeys.OPEN_TRAITS.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new TraitOverviewScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        pending = null;
        traits = List.of();
        occupation = Optional.empty();
        data = TraitData.EMPTY;
    }
}
