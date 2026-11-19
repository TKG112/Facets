package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Changes how fast the player gets hungry and how much food restores.
 * <pre>
 * { "type": "facets:hunger", "exhaustion": 1.25, "food": 0.8 }
 * </pre>
 *
 * @param exhaustion multiplier on hunger drain from sprinting, jumping, fighting...
 * @param food       multiplier on the hunger and saturation restored by eating
 */
public record HungerEffect(float exhaustion, float food) implements TraitEffect {
    public static final MapCodec<HungerEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("exhaustion", 1f).forGetter(HungerEffect::exhaustion),
            Codec.floatRange(0, Float.MAX_VALUE).optionalFieldOf("food", 1f).forGetter(HungerEffect::food)
    ).apply(instance, HungerEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        if (exhaustion != 1) {
            lines.accept(EffectTooltips.percentLine("facets.effect.hunger.exhaustion", exhaustion, false));
        }
        if (food != 1) {
            lines.accept(EffectTooltips.percentLine("facets.effect.hunger.food", food, true));
        }
    }
}
