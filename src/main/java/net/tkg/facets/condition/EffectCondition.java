package net.tkg.facets.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.registry.FacetsRegistries;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A check on the player's current situation, used by facets:conditional effects. Conditions are
 * dispatched by their "type" field like effects, so new ones can be added without touching the format.
 */
public interface EffectCondition {
    Codec<EffectCondition> CODEC = Codec.lazyInitialized(() ->
            FacetsRegistries.CONDITION_TYPES.byNameCodec().dispatch(EffectCondition::codec, Function.identity()));

    MapCodec<? extends EffectCondition> codec();

    boolean test(ServerPlayer player);

    /** Short description for tooltips, read as "When &lt;description&gt;:". */
    Component describe();

    /** Passes this condition and every condition nested inside it to the visitor. */
    default void visit(Consumer<EffectCondition> visitor) {
        visitor.accept(this);
    }
}
