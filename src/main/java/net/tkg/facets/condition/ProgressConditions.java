package net.tkg.facets.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.tkg.facets.data.FacetsData;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitHolder;
import net.tkg.facets.trait.TraitManager;
import net.tkg.facets.trait.TraitRef;
import org.jetbrains.annotations.Nullable;

/** Conditions about the player's progress, mostly used for gain_when / lose_when. */
public final class ProgressConditions {
    private ProgressConditions() {
    }

    /** { "type": "facets:advancement", "advancement": "minecraft:story/enter_the_nether" } */
    public record Advancement(ResourceLocation advancement) implements EffectCondition {
        public static final MapCodec<Advancement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("advancement").forGetter(Advancement::advancement)
        ).apply(i, Advancement::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            AdvancementHolder holder = player.server.getAdvancements().get(advancement);
            return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.advancement", Component.translatableWithFallback(
                    "advancements." + advancement.getPath().replace('/', '.') + ".title", advancement.toString()));
        }
    }

    /** Where a stat condition starts counting from. */
    public enum Since {
        /** Every value the statistic ever reached, across deaths and characters */
        EVER,
        /** Only what the current character did: since first join, or since the last reset (death with onDeath = RESET or /facets reset) */
        CHARACTER;

        /** "ever" or "character"; "selection" is accepted as another name for "character" */
        public static final Codec<Since> CODEC = Codec.STRING.comapFlatMap(value -> switch (value) {
            case "ever" -> DataResult.success(EVER);
            case "character", "selection" -> DataResult.success(CHARACTER);
            default -> DataResult.error(() -> "Unknown since \"" + value + "\", expected \"ever\" or \"character\"");
        }, since -> since == EVER ? "ever" : "character");
    }

    /**
     * { "type": "facets:stat", "stat_type": "minecraft:custom", "stat": "minecraft:mob_kills", "at_least": 100, "since": "character" }
     * Distances are in centimeters, times in ticks, like in the statistics screen's raw values.
     */
    public record StatValue(ResourceLocation statType, ResourceLocation stat, int atLeast, Since since) implements EffectCondition {
        public static final MapCodec<StatValue> CODEC = RecordCodecBuilder.<StatValue>mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("stat_type").forGetter(StatValue::statType),
                ResourceLocation.CODEC.fieldOf("stat").forGetter(StatValue::stat),
                Codec.INT.fieldOf("at_least").forGetter(StatValue::atLeast),
                Since.CODEC.optionalFieldOf("since", Since.EVER).forGetter(StatValue::since)
        ).apply(i, StatValue::new)).validate(StatValue::check);

        private static DataResult<StatValue> check(StatValue condition) {
            StatType<?> type = BuiltInRegistries.STAT_TYPE.get(condition.statType);
            if (type == null || !BuiltInRegistries.STAT_TYPE.containsKey(condition.statType)) {
                return DataResult.error(() -> "Unknown stat type " + condition.statType);
            }
            if (!type.getRegistry().containsKey(condition.stat)) {
                return DataResult.error(() -> "Unknown stat " + condition.stat + " for stat type " + condition.statType);
            }
            return DataResult.success(condition);
        }

        @Nullable
        private Stat<?> resolve() {
            StatType<?> type = BuiltInRegistries.STAT_TYPE.get(statType);
            return type == null ? null : resolve(type, stat);
        }

        @Nullable
        private static <T> Stat<T> resolve(StatType<T> type, ResourceLocation id) {
            if (!type.getRegistry().containsKey(id)) {
                return null;
            }
            return type.get(type.getRegistry().get(id));
        }

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        /** Identifies the statistic in the player's stat baseline. */
        public String baselineKey() {
            return statType + "|" + stat;
        }

        public int currentValue(ServerPlayer player) {
            Stat<?> resolved = resolve();
            return resolved == null ? 0 : player.getStats().getValue(resolved);
        }

        @Override
        public boolean test(ServerPlayer player) {
            if (resolve() == null) {
                return false;
            }
            int value = currentValue(player);
            if (since == Since.CHARACTER) {
                value -= TraitManager.statBaseline(player, baselineKey(), value);
            }
            return value >= atLeast;
        }

        @Override
        public Component describe() {
            StatType<?> type = BuiltInRegistries.STAT_TYPE.get(statType);
            Component what = type == null ? Component.literal(stat.toString()) : valueName(type);
            Component text = statType.equals(BuiltInRegistries.STAT_TYPE.getKey(net.minecraft.stats.Stats.CUSTOM))
                    ? Component.translatable("facets.condition.stat.custom", what, atLeast)
                    : Component.translatable("facets.condition.stat", type.getDisplayName(), what, atLeast);
            return since == Since.CHARACTER ? Component.translatable("facets.condition.stat.character", text) : text;
        }

        private Component valueName(StatType<?> type) {
            Object value = type.getRegistry().get(stat);
            if (value instanceof EntityType<?> entity) {
                return entity.getDescription();
            }
            if (value instanceof Item item) {
                return item.getDescription();
            }
            if (value instanceof Block block) {
                return block.getName();
            }
            return Component.translatableWithFallback("stat." + stat.getNamespace() + "." + stat.getPath(), stat.toString());
        }
    }

    /** { "type": "facets:has_trait", "trait": "mypack:strong" } (a trait or a #tag) */
    public record HasTrait(TraitRef trait) implements EffectCondition {
        public static final MapCodec<HasTrait> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                TraitRef.CODEC.fieldOf("trait").forGetter(HasTrait::trait)
        ).apply(i, HasTrait::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            for (ResourceLocation id : TraitManager.effectiveTraits(player)) {
                TraitHolder holder = TraitManager.data().holder(id);
                if (holder != null && trait.matches(holder)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Component describe() {
            Component name = Component.literal(trait.toString());
            if (!trait.tag()) {
                Trait found = FacetsData.forDisplay().trait(trait.location());
                if (found != null) {
                    name = found.name();
                }
            }
            return Component.translatable("facets.condition.has_trait", name);
        }
    }
}
