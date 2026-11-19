package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.registry.FacetsRegistries;

import java.util.function.Function;

/**
 * Something a trait does to the player. Effects are dispatched by their "type" field,
 * so new effect types can be added without touching the trait format.
 */
public interface TraitEffect {
    Codec<TraitEffect> CODEC = Codec.lazyInitialized(() ->
            FacetsRegistries.EFFECT_TYPES.byNameCodec().dispatch(TraitEffect::codec, Function.identity()));

    MapCodec<? extends TraitEffect> codec();

    /**
     * Called whenever the player's traits are (re)applied: login, respawn, datapack reload or a
     * change in traits. Must be safe to call repeatedly.
     */
    void apply(ServerPlayer player, EffectContext context);

    /** Called once when the trait owning this effect is taken away from the player. */
    default void remove(ServerPlayer player, EffectContext context) {
    }
}
