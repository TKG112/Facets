package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.registry.FacetsRegistries;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Something a trait does to the player. Effects are dispatched by their "type" field,
 * so new effect types can be added without touching the trait format.
 * <p>
 * Effects either change the player directly in {@link #apply} (attributes, mob effects), or are
 * looked up by event handlers through {@link #forEachActive} (damage, hunger, experience).
 */
public interface TraitEffect {
    Codec<TraitEffect> CODEC = Codec.lazyInitialized(() ->
            FacetsRegistries.EFFECT_TYPES.byNameCodec().dispatch(TraitEffect::codec, Function.identity()));

    MapCodec<? extends TraitEffect> codec();

    /**
     * Called whenever the player's traits are (re)applied: login, respawn, datapack reload, a change
     * in traits, and periodically when {@link #needsTicking} is true. Must be safe to call repeatedly.
     */
    default void apply(ServerPlayer player, EffectContext context) {
    }

    /** Called when the trait owning this effect is taken away, or a condition around it stops matching. */
    default void remove(ServerPlayer player, EffectContext context) {
    }

    /** True if the effect has to be reapplied regularly, e.g. because it depends on a condition. */
    default boolean needsTicking() {
        return false;
    }

    /** Passes every effect that is currently active, this one included, to the consumer. */
    default void forEachActive(ServerPlayer player, Consumer<TraitEffect> consumer) {
        consumer.accept(this);
    }

    /** Adds lines describing this effect to trait tooltips. Called on the client. */
    default void addTooltip(Consumer<Component> lines) {
    }
}
