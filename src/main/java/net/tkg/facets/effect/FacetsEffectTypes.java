package net.tkg.facets.effect;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.facets.Facets;
import net.tkg.facets.registry.FacetsRegistries;

public final class FacetsEffectTypes {
    public static final DeferredRegister<MapCodec<? extends TraitEffect>> EFFECT_TYPES =
            DeferredRegister.create(FacetsRegistries.EFFECT_TYPE, Facets.MODID);

    public static final DeferredHolder<MapCodec<? extends TraitEffect>, MapCodec<AttributeEffect>> ATTRIBUTE =
            EFFECT_TYPES.register("attribute", () -> AttributeEffect.CODEC);

    private FacetsEffectTypes() {
    }
}
