package net.tkg.facets.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tkg.facets.Facets;
import net.tkg.facets.data.FacetsData;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Facets.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FacetsKeys {
    public static final KeyMapping OPEN_TRAITS = new KeyMapping("key.facets.traits", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.facets");

    private FacetsKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TRAITS);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Tooltips are built from the data the server synced, not the integrated server's copy
        FacetsData.setDisplaySource(FacetsClient::data);
    }
}
