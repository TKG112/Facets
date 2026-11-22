package net.tkg.facets.effect;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.List;
import java.util.function.Consumer;

/**
 * Keeps mob effects from being applied to the player while the trait is held, and clears them when
 * the trait is gained.
 * <pre>
 * { "type": "facets:effect_immunity", "effects": ["minecraft:poison", "minecraft:hunger"] }
 * </pre>
 * "effects" takes an effect id, a list, or a mob effect tag ("#mypack:toxins").
 */
public record EffectImmunity(HolderSet<MobEffect> effects) implements TraitEffect {
    public static final MapCodec<EffectImmunity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("effects").forGetter(EffectImmunity::effects)
    ).apply(instance, EffectImmunity::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    public boolean blocks(Holder<MobEffect> effect) {
        return effects.contains(effect);
    }

    /** Clears effects the player already has; runs whenever traits are (re)applied. */
    @Override
    public void apply(ServerPlayer player, EffectContext context) {
        List<Holder<MobEffect>> active = player.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(this::blocks)
                .toList();
        active.forEach(player::removeEffect);
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        Component names = effects.unwrapKey()
                .<Component>map(tag -> Component.translatableWithFallback(
                        "facets.effect_tag." + tag.location().getNamespace() + "." + tag.location().getPath().replace('/', '.'),
                        "#" + tag.location()))
                .orElseGet(() -> ComponentUtils.formatList(effects.stream().map(holder -> holder.value().getDisplayName()).toList(),
                        Component.literal(", ")));
        lines.accept(Component.translatable("facets.effect.effect_immunity", names).withStyle(ChatFormatting.BLUE));
    }
}
