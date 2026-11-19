package net.tkg.facets;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.tkg.facets.config.FacetsConfig;
import net.tkg.facets.effect.FacetsEffectTypes;
import net.tkg.facets.network.FacetsNetwork;
import net.tkg.facets.player.FacetsAttachments;
import net.tkg.facets.registry.FacetsRegistries;
import org.slf4j.Logger;

@Mod(Facets.MODID)
public class Facets {
    public static final String MODID = "facets";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Facets(IEventBus modBus, ModContainer container) {
        modBus.addListener(FacetsRegistries::onNewRegistry);
        modBus.addListener(FacetsRegistries::onNewDataPackRegistry);
        modBus.addListener(FacetsNetwork::register);

        FacetsEffectTypes.EFFECT_TYPES.register(modBus);
        FacetsAttachments.ATTACHMENT_TYPES.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, FacetsConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
