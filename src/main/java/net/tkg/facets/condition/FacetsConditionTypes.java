package net.tkg.facets.condition;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.facets.Facets;
import net.tkg.facets.registry.FacetsRegistries;

public final class FacetsConditionTypes {
    public static final DeferredRegister<MapCodec<? extends EffectCondition>> CONDITION_TYPES =
            DeferredRegister.create(FacetsRegistries.CONDITION_TYPE, Facets.MODID);

    static {
        CONDITION_TYPES.register("time", () -> Conditions.Time.CODEC);
        CONDITION_TYPES.register("dimension", () -> Conditions.Dimension.CODEC);
        CONDITION_TYPES.register("biome", () -> Conditions.InBiome.CODEC);
        CONDITION_TYPES.register("health", () -> Conditions.Health.CODEC);
        CONDITION_TYPES.register("in_water", () -> Conditions.InWater.CODEC);
        CONDITION_TYPES.register("all_of", () -> Conditions.AllOf.CODEC);
        CONDITION_TYPES.register("any_of", () -> Conditions.AnyOf.CODEC);
        CONDITION_TYPES.register("not", () -> Conditions.Not.CODEC);
        CONDITION_TYPES.register("advancement", () -> ProgressConditions.Advancement.CODEC);
        CONDITION_TYPES.register("stat", () -> ProgressConditions.StatValue.CODEC);
        CONDITION_TYPES.register("has_trait", () -> ProgressConditions.HasTrait.CODEC);
    }

    private FacetsConditionTypes() {
    }
}
