package net.tkg.facets.compat.pmmo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import harmonised.pmmo.api.APIUtils;
import harmonised.pmmo.api.events.XpEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.facets.Facets;
import net.tkg.facets.condition.EffectCondition;
import net.tkg.facets.effect.TraitEffect;
import net.tkg.facets.registry.FacetsRegistries;
import net.tkg.facets.trait.TraitManager;

/** Project MMO integration. Only loaded when pmmo is installed. */
public final class PmmoCompat {
    public static final String MOD_ID = "pmmo";

    private static final DeferredRegister<MapCodec<? extends TraitEffect>> EFFECTS =
            DeferredRegister.create(FacetsRegistries.EFFECT_TYPE, Facets.MODID);
    private static final DeferredRegister<MapCodec<? extends EffectCondition>> CONDITIONS =
            DeferredRegister.create(FacetsRegistries.CONDITION_TYPE, Facets.MODID);

    static {
        EFFECTS.register("pmmo_xp", () -> PmmoXpEffect.CODEC);
        CONDITIONS.register("pmmo_level", () -> SkillLevel.CODEC);
    }

    private PmmoCompat() {
    }

    public static void init(IEventBus modBus) {
        EFFECTS.register(modBus);
        CONDITIONS.register(modBus);
        NeoForge.EVENT_BUS.addListener(PmmoCompat::onXp);
    }

    /** Project MMO applies whatever amount is left in the event after listeners ran. */
    private static void onXp(XpEvent event) {
        if (event.amountAwarded <= 0 || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float multiplier = 1;
        for (PmmoXpEffect effect : TraitManager.activeEffects(player, PmmoXpEffect.class)) {
            if (effect.appliesTo(event.skill)) {
                multiplier *= effect.multiplier();
            }
        }
        if (multiplier == 1) {
            return;
        }
        // Keep fractions fair on average, like the vanilla experience effect
        double scaled = event.amountAwarded * (double) multiplier;
        long whole = Mth.lfloor(scaled);
        if (player.getRandom().nextDouble() < scaled - whole) {
            whole++;
        }
        event.amountAwarded = whole;
    }

    static Component skillName(String skill) {
        String fallback = skill.isEmpty() ? skill : Character.toUpperCase(skill.charAt(0)) + skill.substring(1);
        return Component.translatableWithFallback("pmmo." + skill, fallback);
    }

    /** { "type": "facets:pmmo_level", "skill": "mining", "at_least": 50 } */
    public record SkillLevel(String skill, long atLeast) implements EffectCondition {
        public static final MapCodec<SkillLevel> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("skill").forGetter(SkillLevel::skill),
                Codec.LONG.fieldOf("at_least").forGetter(SkillLevel::atLeast)
        ).apply(i, SkillLevel::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return APIUtils.getLevel(skill, player) >= atLeast;
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.pmmo_level", skillName(skill), atLeast);
        }
    }
}
