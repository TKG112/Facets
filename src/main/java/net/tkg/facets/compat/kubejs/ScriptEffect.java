package net.tkg.facets.compat.kubejs;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.effect.EffectContext;
import net.tkg.facets.effect.TraitEffect;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * An effect handled by a KubeJS server script through FacetsEvents.scriptEffect. Only available
 * when KubeJS is installed.
 * <pre>
 * { "type": "facets:script", "id": "mypack:glow", "tooltip": "Glows in the dark" }
 * </pre>
 */
public record ScriptEffect(ResourceLocation id, Optional<Component> tooltip) implements TraitEffect {
    public static final MapCodec<ScriptEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ScriptEffect::id),
            ComponentSerialization.CODEC.optionalFieldOf("tooltip").forGetter(ScriptEffect::tooltip)
    ).apply(instance, ScriptEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, EffectContext context) {
        post(player, context, true);
    }

    @Override
    public void remove(ServerPlayer player, EffectContext context) {
        post(player, context, false);
    }

    private void post(ServerPlayer player, EffectContext context, boolean applying) {
        if (FacetsJSEvents.SCRIPT_EFFECT.hasListeners()) {
            FacetsJSEvents.SCRIPT_EFFECT.post(new ScriptEffectKubeEvent(player, id, context.traitId(), applying));
        }
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        tooltip.ifPresent(line -> lines.accept(line.copy().withStyle(ChatFormatting.BLUE)));
    }
}
