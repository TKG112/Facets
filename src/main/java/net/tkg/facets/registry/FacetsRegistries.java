package net.tkg.facets.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.tkg.facets.Facets;
import net.tkg.facets.effect.TraitEffect;
import net.tkg.facets.trait.Trait;

public final class FacetsRegistries {
    /** Datapack registry, loaded from data/&lt;namespace&gt;/facets/trait/*.json */
    public static final ResourceKey<Registry<Trait>> TRAIT = ResourceKey.createRegistryKey(Facets.id("trait"));

    /** Static registry of effect types. Compat modules add their own types here. */
    public static final ResourceKey<Registry<MapCodec<? extends TraitEffect>>> EFFECT_TYPE =
            ResourceKey.createRegistryKey(Facets.id("effect_type"));

    public static final Registry<MapCodec<? extends TraitEffect>> EFFECT_TYPES =
            new RegistryBuilder<>(EFFECT_TYPE).create();

    private FacetsRegistries() {
    }

    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(EFFECT_TYPES);
    }

    public static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        // Synced so the client can show names, descriptions and costs on the selection screen
        event.dataPackRegistry(TRAIT, Trait.CODEC, Trait.CODEC);
    }
}
