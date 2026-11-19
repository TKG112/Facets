package net.tkg.facets;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.tkg.facets.compat.kubejs.KubeJSCompat;
import net.tkg.facets.compat.origins.OriginsCompat;
import net.tkg.facets.compat.pmmo.PmmoCompat;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.tkg.facets.condition.FacetsConditionTypes;
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
        modBus.addListener(FacetsNetwork::register);

        FacetsEffectTypes.EFFECT_TYPES.register(modBus);
        FacetsConditionTypes.CONDITION_TYPES.register(modBus);
        FacetsAttachments.ATTACHMENT_TYPES.register(modBus);

        container.registerConfig(ModConfig.Type.SERVER, FacetsConfig.SPEC);

        // Integrations are only class-loaded when their mod is present
        ModList mods = ModList.get();
        if (mods.isLoaded(PmmoCompat.MOD_ID)) {
            PmmoCompat.init(modBus);
        }
        if (mods.isLoaded(OriginsCompat.MOD_ID)) {
            OriginsCompat.init(modBus);
        }
        if (mods.isLoaded(KubeJSCompat.MOD_ID)) {
            KubeJSCompat.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
