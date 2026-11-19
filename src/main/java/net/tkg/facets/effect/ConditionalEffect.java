package net.tkg.facets.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.condition.EffectCondition;

import java.util.List;
import java.util.function.Consumer;

/**
 * Runs other effects only while a condition matches. Checked every half second.
 * <pre>
 * { "type": "facets:conditional",
 *   "condition": { "type": "facets:time", "period": "night" },
 *   "effects": [ { "type": "facets:mob_effect", "effect": "minecraft:night_vision" } ] }
 * </pre>
 */
public record ConditionalEffect(EffectCondition condition, List<TraitEffect> effects) implements TraitEffect {
    public static final MapCodec<ConditionalEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EffectCondition.CODEC.fieldOf("condition").forGetter(ConditionalEffect::condition),
            TraitEffect.CODEC.listOf().fieldOf("effects").forGetter(ConditionalEffect::effects)
    ).apply(instance, ConditionalEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, EffectContext context) {
        boolean active = condition.test(player);
        for (int i = 0; i < effects.size(); i++) {
            if (active) {
                effects.get(i).apply(player, context.child(i));
            } else {
                effects.get(i).remove(player, context.child(i));
            }
        }
    }

    @Override
    public void remove(ServerPlayer player, EffectContext context) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).remove(player, context.child(i));
        }
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void forEachActive(ServerPlayer player, Consumer<TraitEffect> consumer) {
        if (condition.test(player)) {
            effects.forEach(effect -> effect.forEachActive(player, consumer));
        }
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        lines.accept(Component.translatable("facets.effect.conditional", condition.describe()).withStyle(ChatFormatting.GRAY));
        effects.forEach(effect -> effect.addTooltip(line -> lines.accept(Component.literal("  ").append(line))));
    }
}
