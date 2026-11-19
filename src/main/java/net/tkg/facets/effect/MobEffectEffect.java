package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.function.Consumer;

/**
 * Keeps a mob effect on the player while the trait is held. Milk removes it for a moment, it comes
 * back on the next check.
 * <pre>
 * { "type": "facets:mob_effect", "effect": "minecraft:night_vision", "amplifier": 0, "show_icon": true, "particles": false }
 * </pre>
 */
public record MobEffectEffect(Holder<MobEffect> effect, int amplifier, boolean showIcon, boolean particles) implements TraitEffect {
    public static final MapCodec<MobEffectEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(MobEffectEffect::effect),
            Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(MobEffectEffect::amplifier),
            Codec.BOOL.optionalFieldOf("show_icon", true).forGetter(MobEffectEffect::showIcon),
            Codec.BOOL.optionalFieldOf("particles", false).forGetter(MobEffectEffect::particles)
    ).apply(instance, MobEffectEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, EffectContext context) {
        MobEffectInstance current = player.getEffect(effect);
        boolean ours = current != null && current.isInfiniteDuration() && current.getAmplifier() == amplifier;
        if (current == null || (!ours && current.getAmplifier() <= amplifier)) {
            // A stronger potion stays on top; vanilla keeps this one hidden underneath until it runs out
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, particles, showIcon));
        }
    }

    @Override
    public void remove(ServerPlayer player, EffectContext context) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.isInfiniteDuration() && current.getAmplifier() == amplifier) {
            player.removeEffect(effect);
        }
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        MutableComponent name = effect.value().getDisplayName().copy();
        if (amplifier > 0) {
            name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + amplifier));
        }
        lines.accept(Component.translatable("facets.effect.mob_effect", name)
                .withStyle(effect.value().getCategory() == MobEffectCategory.HARMFUL
                        ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.BLUE));
    }
}
