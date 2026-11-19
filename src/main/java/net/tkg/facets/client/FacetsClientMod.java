package net.tkg.facets.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.tkg.facets.Facets;
import net.tkg.facets.client.theme.ThemeLoader;

/** Client-only entry point. */
@Mod(value = Facets.MODID, dist = Dist.CLIENT)
public class FacetsClientMod {
    public FacetsClientMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, FacetsClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(FacetsClientMod::onRegisterReloadListeners);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ThemeLoader());
    }
}
