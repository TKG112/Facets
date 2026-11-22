package net.tkg.facets.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Built-in condition types. */
public final class Conditions {
    private Conditions() {
    }

    /** Name for a tag or for each entry of a holder set, with a translation key packs can fill in. */
    static <T> Component holderSetName(HolderSet<T> set, String tagPrefix, String entryPrefix) {
        return set.unwrapKey()
                .<Component>map(tag -> Component.translatableWithFallback(
                        tagPrefix + "." + tag.location().getNamespace() + "." + tag.location().getPath().replace('/', '.'),
                        "#" + tag.location()))
                .orElseGet(() -> {
                    List<Component> names = set.stream()
                            .map(holder -> holder.unwrapKey().map(ResourceKey::location).orElse(null))
                            .filter(Objects::nonNull)
                            .<Component>map(id -> Component.translatableWithFallback(
                                    entryPrefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.getPath()))
                            .toList();
                    return ComponentUtils.formatList(names, Component.literal(", "));
                });
    }

    /** { "type": "facets:time", "period": "day" | "night" } */
    public record Time(Period period) implements EffectCondition {
        public enum Period implements StringRepresentable {
            DAY("day"),
            NIGHT("night");

            public static final Codec<Period> CODEC = StringRepresentable.fromEnum(Period::values);
            private final String name;

            Period(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return name;
            }
        }

        public static final MapCodec<Time> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Period.CODEC.fieldOf("period").forGetter(Time::period)
        ).apply(i, Time::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            // Dimensions without a day cycle (Nether, End) count as neither
            if (player.level().dimensionType().hasFixedTime()) {
                return false;
            }
            // Based on the clock rather than Level#isDay, which also turns to night during thunderstorms
            long time = player.level().getDayTime() % 24000;
            boolean night = time >= 13000 && time < 23000;
            return (period == Period.NIGHT) == night;
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.time." + period.getSerializedName());
        }
    }

    /** { "type": "facets:dimension", "dimension": "minecraft:the_nether" } */
    public record Dimension(ResourceKey<Level> dimension) implements EffectCondition {
        public static final MapCodec<Dimension> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Dimension::dimension)
        ).apply(i, Dimension::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return player.level().dimension().equals(dimension);
        }

        @Override
        public Component describe() {
            ResourceLocation id = dimension.location();
            return Component.translatable("facets.condition.in", Component.translatableWithFallback(
                    "facets.dimension." + id.getNamespace() + "." + id.getPath(), id.getPath()));
        }
    }

    /** { "type": "facets:biome", "biomes": "#minecraft:is_forest" } (a biome, list of biomes or tag) */
    public record InBiome(HolderSet<Biome> biomes) implements EffectCondition {
        public static final MapCodec<InBiome> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(InBiome::biomes)
        ).apply(i, InBiome::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            Holder<Biome> biome = player.level().getBiome(player.blockPosition());
            return biomes.contains(biome);
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.in", holderSetName(biomes, "facets.biome_tag", "biome"));
        }
    }

    /** { "type": "facets:health", "below": 0.5 } or "above"; fractions of max health. */
    public record Health(Optional<Float> below, Optional<Float> above) implements EffectCondition {
        public static final MapCodec<Health> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("below").forGetter(Health::below),
                Codec.FLOAT.optionalFieldOf("above").forGetter(Health::above)
        ).apply(i, Health::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            float fraction = player.getHealth() / player.getMaxHealth();
            return below.map(b -> fraction < b).orElse(true) && above.map(a -> fraction > a).orElse(true);
        }

        @Override
        public Component describe() {
            if (below.isPresent() && above.isPresent()) {
                return Component.translatable("facets.condition.health.between", percent(above.get()), percent(below.get()));
            }
            if (below.isPresent()) {
                return Component.translatable("facets.condition.health.below", percent(below.get()));
            }
            return Component.translatable("facets.condition.health.above", percent(above.orElse(0f)));
        }

        private static String percent(float fraction) {
            return Math.round(fraction * 100) + "%";
        }
    }

    /** { "type": "facets:in_water" } */
    public record InWater() implements EffectCondition {
        public static final InWater INSTANCE = new InWater();
        public static final MapCodec<InWater> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return player.isInWater();
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.in_water");
        }
    }

    /** { "type": "facets:all_of", "conditions": [...] } */
    public record AllOf(List<EffectCondition> conditions) implements EffectCondition {
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                EffectCondition.CODEC.listOf().fieldOf("conditions").forGetter(AllOf::conditions)
        ).apply(i, AllOf::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return conditions.stream().allMatch(c -> c.test(player));
        }

        @Override
        public Component describe() {
            return join(conditions, "facets.condition.and");
        }

        @Override
        public void visit(java.util.function.Consumer<EffectCondition> visitor) {
            visitor.accept(this);
            conditions.forEach(c -> c.visit(visitor));
        }
    }

    /** { "type": "facets:any_of", "conditions": [...] } */
    public record AnyOf(List<EffectCondition> conditions) implements EffectCondition {
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                EffectCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::conditions)
        ).apply(i, AnyOf::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return conditions.stream().anyMatch(c -> c.test(player));
        }

        @Override
        public Component describe() {
            return join(conditions, "facets.condition.or");
        }

        @Override
        public void visit(java.util.function.Consumer<EffectCondition> visitor) {
            visitor.accept(this);
            conditions.forEach(c -> c.visit(visitor));
        }
    }

    /** { "type": "facets:not", "condition": {...} } */
    public record Not(EffectCondition condition) implements EffectCondition {
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                EffectCondition.CODEC.fieldOf("condition").forGetter(Not::condition)
        ).apply(i, Not::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return !condition.test(player);
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.not", condition.describe());
        }

        @Override
        public void visit(java.util.function.Consumer<EffectCondition> visitor) {
            visitor.accept(this);
            condition.visit(visitor);
        }
    }

    private static Component join(List<EffectCondition> conditions, String separatorKey) {
        if (conditions.isEmpty()) {
            return Component.empty();
        }
        var result = conditions.get(0).describe().copy();
        for (int i = 1; i < conditions.size(); i++) {
            result = Component.translatable(separatorKey, result, conditions.get(i).describe());
        }
        return result;
    }
}
