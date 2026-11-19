package net.tkg.facets.compat.kubejs;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.facets.Facets;
import net.tkg.facets.api.event.OccupationChangeEvent;
import net.tkg.facets.api.event.TraitChangeEvent;
import net.tkg.facets.effect.TraitEffect;
import net.tkg.facets.registry.FacetsRegistries;

/** KubeJS integration. Only loaded when kubejs is installed; the plugin itself is in kubejs.plugins.txt. */
public final class KubeJSCompat {
    public static final String MOD_ID = "kubejs";

    private static final DeferredRegister<MapCodec<? extends TraitEffect>> EFFECTS =
            DeferredRegister.create(FacetsRegistries.EFFECT_TYPE, Facets.MODID);

    static {
        EFFECTS.register("script", () -> ScriptEffect.CODEC);
    }

    private KubeJSCompat() {
    }

    public static void init(IEventBus modBus) {
        EFFECTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(KubeJSCompat::onTraitAdded);
        NeoForge.EVENT_BUS.addListener(KubeJSCompat::onTraitRemoved);
        NeoForge.EVENT_BUS.addListener(KubeJSCompat::onOccupationChanged);
    }

    private static void onTraitAdded(TraitChangeEvent.Added event) {
        if (FacetsJSEvents.TRAIT_ADDED.hasListeners()) {
            FacetsJSEvents.TRAIT_ADDED.post(new TraitChangedKubeEvent(event.getPlayer(), event.getTrait(), event.getCause()));
        }
    }

    private static void onTraitRemoved(TraitChangeEvent.Removed event) {
        if (FacetsJSEvents.TRAIT_REMOVED.hasListeners()) {
            FacetsJSEvents.TRAIT_REMOVED.post(new TraitChangedKubeEvent(event.getPlayer(), event.getTrait(), event.getCause()));
        }
    }

    private static void onOccupationChanged(OccupationChangeEvent event) {
        if (FacetsJSEvents.OCCUPATION_CHANGED.hasListeners()) {
            FacetsJSEvents.OCCUPATION_CHANGED.post(new OccupationChangedKubeEvent(event.getPlayer(), event.getPrevious(),
                    event.getCurrent(), event.getCause()));
        }
    }
}
