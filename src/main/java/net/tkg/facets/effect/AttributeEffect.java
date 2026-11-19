package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Adds an attribute modifier while the trait is held.
 * <pre>
 * { "type": "facets:attribute", "attribute": "minecraft:generic.max_health", "amount": 4, "operation": "add_value" }
 * </pre>
 */
public record AttributeEffect(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation)
        implements TraitEffect {

    public static final MapCodec<AttributeEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(AttributeEffect::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeEffect::amount),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE)
                    .forGetter(AttributeEffect::operation)
    ).apply(instance, AttributeEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(ServerPlayer player, EffectContext context) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        ResourceLocation id = context.modifierId();
        context.appliedModifiers().add(id);

        // Only touch the modifier when it actually changed. Removing and re-adding max health
        // would clamp the player's current health every time traits are refreshed.
        AttributeModifier wanted = new AttributeModifier(id, amount, operation);
        if (!wanted.equals(instance.getModifier(id))) {
            // Permanent so the modifier is saved with the player and loaded before health is,
            // otherwise extra max health would be lost on every relog.
            instance.addOrReplacePermanentModifier(wanted);
        }
    }
}
