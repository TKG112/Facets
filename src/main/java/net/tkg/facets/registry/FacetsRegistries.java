package net.tkg.facets.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.tkg.facets.Facets;
import net.tkg.facets.condition.EffectCondition;
import net.tkg.facets.effect.TraitEffect;

/**
 * Static registries for effect and condition types. Traits, categories and occupations themselves
 * aren't registries, so they can be reloaded with /reload; see TraitDataLoader.
 */
public final class FacetsRegistries {
    /** Static registry of effect types. Compat modules add their own types here. */
    public static final ResourceKey<Registry<MapCodec<? extends TraitEffect>>> EFFECT_TYPE =
            ResourceKey.createRegistryKey(Facets.id("effect_type"));

    public static final Registry<MapCodec<? extends TraitEffect>> EFFECT_TYPES =
            new RegistryBuilder<>(EFFECT_TYPE).create();

    /** Static registry of condition types used by facets:conditional effects. */
    public static final ResourceKey<Registry<MapCodec<? extends EffectCondition>>> CONDITION_TYPE =
            ResourceKey.createRegistryKey(Facets.id("condition_type"));

    public static final Registry<MapCodec<? extends EffectCondition>> CONDITION_TYPES =
            new RegistryBuilder<>(CONDITION_TYPE).create();

    private FacetsRegistries() {
    }

    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(EFFECT_TYPES);
        event.register(CONDITION_TYPES);
    }
}
