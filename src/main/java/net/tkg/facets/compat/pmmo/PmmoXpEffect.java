package net.tkg.facets.compat.pmmo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.tkg.facets.effect.TraitEffect;

import java.util.List;
import java.util.function.Consumer;

/**
 * Multiplies Project MMO experience. Only available when Project MMO is installed.
 * <pre>
 * { "type": "facets:pmmo_xp", "skills": ["mining", "excavation"], "multiplier": 1.25 }
 * </pre>
 *
 * @param skills skills affected; empty means every skill
 */
public record PmmoXpEffect(List<String> skills, float multiplier) implements TraitEffect {
    public static final MapCodec<PmmoXpEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("skills", List.of()).forGetter(PmmoXpEffect::skills),
            Codec.floatRange(0, Float.MAX_VALUE).fieldOf("multiplier").forGetter(PmmoXpEffect::multiplier)
    ).apply(instance, PmmoXpEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    public boolean appliesTo(String skill) {
        return skills.isEmpty() || skills.contains(skill);
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        String percent = (multiplier >= 1 ? "+" : "") + Math.round((multiplier - 1) * 100) + "%";
        Component line = skills.isEmpty()
                ? Component.translatable("facets.effect.pmmo_xp.all", percent)
                : Component.translatable("facets.effect.pmmo_xp", percent,
                        net.minecraft.network.chat.ComponentUtils.formatList(skills.stream().map(PmmoCompat::skillName).toList(),
                                Component.literal(", ")));
        lines.accept(line.copy().withStyle(multiplier >= 1 ? net.minecraft.ChatFormatting.BLUE : net.minecraft.ChatFormatting.RED));
    }
}
