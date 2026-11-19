package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Multiplies experience points the player gains.
 * <pre>
 * { "type": "facets:experience", "multiplier": 1.2 }
 * </pre>
 */
public record ExperienceEffect(float multiplier) implements TraitEffect {
    public static final MapCodec<ExperienceEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0, Float.MAX_VALUE).fieldOf("multiplier").forGetter(ExperienceEffect::multiplier)
    ).apply(instance, ExperienceEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        lines.accept(EffectTooltips.percentLine("facets.effect.experience", multiplier, true));
    }
}
