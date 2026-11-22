package net.tkg.facets.effect;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.facets.Facets;
import net.tkg.facets.registry.FacetsRegistries;

public final class FacetsEffectTypes {
    public static final DeferredRegister<MapCodec<? extends TraitEffect>> EFFECT_TYPES =
            DeferredRegister.create(FacetsRegistries.EFFECT_TYPE, Facets.MODID);

    static {
        EFFECT_TYPES.register("attribute", () -> AttributeEffect.CODEC);
        EFFECT_TYPES.register("mob_effect", () -> MobEffectEffect.CODEC);
        EFFECT_TYPES.register("damage", () -> DamageEffect.CODEC);
        EFFECT_TYPES.register("hunger", () -> HungerEffect.CODEC);
        EFFECT_TYPES.register("experience", () -> ExperienceEffect.CODEC);
        EFFECT_TYPES.register("conditional", () -> ConditionalEffect.CODEC);
        EFFECT_TYPES.register("effect_immunity", () -> EffectImmunity.CODEC);
    }

    private FacetsEffectTypes() {
    }
}
