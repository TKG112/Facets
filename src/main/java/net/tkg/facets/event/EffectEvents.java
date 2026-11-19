package net.tkg.facets.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tkg.facets.Facets;
import net.tkg.facets.effect.DamageEffect;
import net.tkg.facets.effect.ExperienceEffect;
import net.tkg.facets.effect.HungerEffect;
import net.tkg.facets.trait.TraitManager;

import java.util.Map;
import java.util.WeakHashMap;

/** Applies the effects that work through game events instead of changing the player directly. */
@EventBusSubscriber(modid = Facets.MODID)
public final class EffectEvents {
    /** How often conditional and mob effects are rechecked, in ticks. */
    private static final int REFRESH_INTERVAL = 10;
    /** How often gain_when / lose_when conditions are checked, in ticks. */
    private static final int TRIGGER_INTERVAL = 20;
    /** Exhaustion is turned into hunger once it passes this value, see FoodData#tick. */
    private static final float EXHAUSTION_STEP = 4.0F;

    private static final Map<ServerPlayer, Float> LAST_EXHAUSTION = new WeakHashMap<>();

    private EffectEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % REFRESH_INTERVAL == 0 && TraitManager.needsTicking(player)) {
            TraitManager.refresh(player, false);
        }
        if (player.tickCount % TRIGGER_INTERVAL == 0) {
            TraitManager.checkTriggers(player);
        }
        scaleExhaustion(player);
    }

    /**
     * There's no event for exhaustion, so the change since last tick is measured and scaled.
     * When exhaustion passes 4 it wraps around and a point of saturation or hunger is taken.
     */
    private static void scaleExhaustion(ServerPlayer player) {
        FoodData food = player.getFoodData();
        float current = food.getExhaustionLevel();
        Float last = LAST_EXHAUSTION.put(player, current);
        if (last == null) {
            return;
        }
        float added = current >= last ? current - last : current + EXHAUSTION_STEP - last;
        if (added <= 0) {
            return;
        }

        float multiplier = 1;
        for (HungerEffect effect : TraitManager.activeEffects(player, HungerEffect.class)) {
            multiplier *= effect.exhaustion();
        }
        if (multiplier == 1) {
            return;
        }

        float extra = added * (multiplier - 1);
        if (extra > 0) {
            food.addExhaustion(extra);
        } else {
            food.setExhaustion(Math.max(0, current + extra));
        }
        LAST_EXHAUSTION.put(player, food.getExhaustionLevel());
    }

    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodProperties properties = event.getItem().get(DataComponents.FOOD);
        if (properties == null) {
            return;
        }
        float multiplier = 1;
        for (HungerEffect effect : TraitManager.activeEffects(player, HungerEffect.class)) {
            multiplier *= effect.food();
        }
        if (multiplier == 1) {
            return;
        }

        FoodData food = player.getFoodData();
        int foodLevel = Mth.clamp(food.getFoodLevel() + Math.round(properties.nutrition() * (multiplier - 1)), 0, 20);
        food.setFoodLevel(foodLevel);
        food.setSaturation(Mth.clamp(food.getSaturationLevel() + properties.saturation() * (multiplier - 1), 0, foodLevel));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        float multiplier = 1;
        if (event.getEntity() instanceof ServerPlayer target) {
            multiplier *= damageMultiplier(target, source, DamageEffect.Direction.TAKEN);
        }
        if (source.getEntity() instanceof ServerPlayer attacker) {
            multiplier *= damageMultiplier(attacker, source, DamageEffect.Direction.DEALT);
        }
        if (multiplier != 1) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    private static float damageMultiplier(ServerPlayer player, DamageSource source, DamageEffect.Direction direction) {
        float multiplier = 1;
        for (DamageEffect effect : TraitManager.activeEffects(player, DamageEffect.class)) {
            if (effect.direction() == direction && effect.matches(source)) {
                multiplier *= effect.multiplier();
            }
        }
        return multiplier;
    }

    @SubscribeEvent
    public static void onExperience(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        float multiplier = 1;
        for (ExperienceEffect effect : TraitManager.activeEffects(player, ExperienceEffect.class)) {
            multiplier *= effect.multiplier();
        }
        if (multiplier == 1) {
            return;
        }
        // Keep fractions fair on average: 1 xp at 1.5x gives 1 or 2 with equal odds
        float scaled = event.getAmount() * multiplier;
        int whole = Mth.floor(scaled);
        if (player.getRandom().nextFloat() < scaled - whole) {
            whole++;
        }
        event.setAmount(whole);
    }
}
