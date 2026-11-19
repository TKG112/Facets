package net.tkg.facets.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Multiplies damage the player takes or deals, optionally only for some damage types.
 * <pre>
 * { "type": "facets:damage", "direction": "taken", "damage_types": "#minecraft:is_fire", "multiplier": 0.5 }
 * </pre>
 */
public record DamageEffect(Direction direction, Optional<HolderSet<DamageType>> damageTypes, float multiplier) implements TraitEffect {
    public enum Direction implements StringRepresentable {
        TAKEN("taken"),
        DEALT("dealt");

        public static final Codec<Direction> CODEC = StringRepresentable.fromEnum(Direction::values);
        private final String name;

        Direction(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<DamageEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Direction.CODEC.optionalFieldOf("direction", Direction.TAKEN).forGetter(DamageEffect::direction),
            RegistryCodecs.homogeneousList(Registries.DAMAGE_TYPE).optionalFieldOf("damage_types").forGetter(DamageEffect::damageTypes),
            Codec.floatRange(0, Float.MAX_VALUE).fieldOf("multiplier").forGetter(DamageEffect::multiplier)
    ).apply(instance, DamageEffect::new));

    @Override
    public MapCodec<? extends TraitEffect> codec() {
        return CODEC;
    }

    public boolean matches(DamageSource source) {
        return damageTypes.map(types -> types.contains(source.typeHolder())).orElse(true);
    }

    @Override
    public void addTooltip(Consumer<Component> lines) {
        boolean taken = direction == Direction.TAKEN;
        String key = "facets.effect.damage." + direction.getSerializedName();
        if (damageTypes.isEmpty()) {
            lines.accept(EffectTooltips.percentLine(key, multiplier, !taken));
            return;
        }
        HolderSet<DamageType> types = damageTypes.get();
        Component typeName = types.unwrapKey()
                .<Component>map(tag -> Component.translatableWithFallback(
                        "facets.damage_tag." + tag.location().getNamespace() + "." + tag.location().getPath(),
                        "#" + tag.location()))
                .orElseGet(() -> Component.literal(String.join(", ", types.stream()
                        .map(h -> h.unwrapKey().map(ResourceKey::location).map(id -> id.getPath()).orElse("?"))
                        .toList())));
        lines.accept(EffectTooltips.percentLine(key + "_typed", multiplier, !taken, typeName));
    }
}
