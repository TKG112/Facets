package net.tkg.facets.trait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.tkg.facets.Facets;
import net.tkg.facets.api.ChangeCause;
import net.tkg.facets.api.event.OccupationChangeEvent;
import net.tkg.facets.api.event.TraitChangeEvent;
import net.tkg.facets.config.FacetsConfig;
import net.tkg.facets.data.FacetsData;
import net.tkg.facets.data.TraitData;
import net.tkg.facets.effect.EffectContext;
import net.tkg.facets.effect.TraitEffect;
import net.tkg.facets.network.OpenSelectionPayload;
import net.tkg.facets.network.SelectionResultPayload;
import net.tkg.facets.network.SyncDataPayload;
import net.tkg.facets.network.SyncTraitsPayload;
import net.tkg.facets.network.TraitNotifyPayload;
import net.tkg.facets.player.FacetsAttachments;
import net.tkg.facets.player.PlayerFacets;

import java.util.ArrayList;
import net.tkg.facets.condition.ProgressConditions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Server-side entry point for reading and changing a player's traits. */
public final class TraitManager {
    /** Why {@link #openSelection} did or didn't open the screen. */
    public enum OpenResult {
        OPENED,
        /** Selection mode is NONE. */
        DISABLED,
        /** No selectable traits and no occupations are loaded. */
        NOTHING_TO_PICK
    }

    private TraitManager() {
    }

    public static TraitData data() {
        return FacetsData.get();
    }

    public static PlayerFacets get(ServerPlayer player) {
        return player.getData(FacetsAttachments.PLAYER_FACETS);
    }

    private static void set(ServerPlayer player, PlayerFacets data) {
        player.setData(FacetsAttachments.PLAYER_FACETS, data);
    }

    /**
     * Every trait the player holds: their own traits plus the free traits of their occupation, minus
     * the occupation traits they lost during play.
     */
    public static List<ResourceLocation> effectiveTraits(ServerPlayer player) {
        PlayerFacets playerData = get(player);
        Set<ResourceLocation> result = new LinkedHashSet<>(playerData.traits());
        playerData.occupation().map(data()::occupation).ifPresent(occupation -> occupation.traits().stream()
                .filter(id -> !playerData.lostOccupationTraits().contains(id))
                .forEach(result::add));
        return List.copyOf(result);
    }

    /** Free traits of the player's occupation they still hold. */
    private static List<ResourceLocation> heldOccupationTraits(PlayerFacets playerData) {
        Occupation occupation = data().occupation(playerData.occupation().orElse(null));
        if (occupation == null) {
            return List.of();
        }
        return occupation.traits().stream().filter(id -> !playerData.lostOccupationTraits().contains(id)).toList();
    }

    /**
     * Takes a trait away, ignoring selection rules: the player's own trait, or a free occupation trait
     * marked removable_from_occupation.
     *
     * @return false if the player doesn't hold the trait, or it's an occupation trait that can't be removed
     */
    public static boolean removeTrait(ServerPlayer player, ResourceLocation id, ChangeCause cause) {
        PlayerFacets playerData = get(player);
        List<ResourceLocation> own = new ArrayList<>(playerData.traits());
        if (own.remove(id)) {
            setTraits(player, own, cause);
            return true;
        }
        Trait trait = data().trait(id);
        if (trait != null && trait.removableFromOccupation() && heldOccupationTraits(playerData).contains(id)) {
            List<ResourceLocation> lost = new ArrayList<>(playerData.lostOccupationTraits());
            lost.add(id);
            setState(player, own, playerData.occupation(), lost, cause);
            return true;
        }
        return false;
    }

    /**
     * The value a statistic had when the player's current character started. Statistics a pack
     * started using later get their baseline the first time they're checked.
     */
    public static int statBaseline(ServerPlayer player, String key, int currentValue) {
        PlayerFacets playerData = get(player);
        Integer baseline = playerData.statBaseline().get(key);
        if (baseline != null) {
            return baseline;
        }
        Map<String, Integer> updated = new HashMap<>(playerData.statBaseline());
        updated.put(key, currentValue);
        set(player, playerData.withStatBaseline(updated));
        return currentValue;
    }

    /** Records the current value of every statistic a "since": "character" stat condition uses. */
    private static void snapshotStats(ServerPlayer player) {
        Map<String, Integer> baseline = new HashMap<>();
        for (TraitHolder holder : data().holders()) {
            holder.value().visitConditions(condition -> {
                if (condition instanceof ProgressConditions.StatValue stat && stat.since() == ProgressConditions.Since.CHARACTER) {
                    baseline.put(stat.baselineKey(), stat.currentValue(player));
                }
            });
        }
        set(player, get(player).withStatBaseline(baseline));
    }

    /** Starts a new character: removes all traits and the occupation, and restarts "since": "character" stats. */
    public static void resetCharacter(ServerPlayer player) {
        setState(player, List.of(), Optional.empty(), List.of(), ChangeCause.RESET);
        snapshotStats(player);
    }

    /** Traits the player holds that can't be picked on the screen (given by commands or other mods). */
    private static List<ResourceLocation> grantedTraits(ServerPlayer player) {
        TraitData data = data();
        return get(player).traits().stream().filter(id -> {
            Trait trait = data.trait(id);
            return trait == null || !trait.selectable();
        }).toList();
    }

    /** Replaces the player's traits, ignoring selection rules. */
    public static void setTraits(ServerPlayer player, List<ResourceLocation> traits, ChangeCause cause) {
        setState(player, traits, get(player).occupation(), cause);
    }

    /**
     * Replaces the player's traits and occupation, ignoring selection rules. Fires trait and
     * occupation change events, and tells the player about it when the cause calls for it.
     */
    public static void setState(ServerPlayer player, List<ResourceLocation> traits, Optional<ResourceLocation> occupation,
                                ChangeCause cause) {
        // A new occupation comes with all its traits, including ones lost under the previous one
        List<ResourceLocation> lost = occupation.equals(get(player).occupation()) ? get(player).lostOccupationTraits() : List.of();
        setState(player, traits, occupation, lost, cause);
    }

    private static void setState(ServerPlayer player, List<ResourceLocation> traits, Optional<ResourceLocation> occupation,
                                 List<ResourceLocation> lostOccupationTraits, ChangeCause cause) {
        List<ResourceLocation> before = effectiveTraits(player);
        Optional<ResourceLocation> previousOccupation = get(player).occupation();
        set(player, get(player).withTraits(traits).withOccupation(occupation).withLostOccupationTraits(lostOccupationTraits));
        List<ResourceLocation> after = effectiveTraits(player);

        TraitData data = data();
        List<ResourceLocation> removed = before.stream().filter(id -> !after.contains(id)).toList();
        List<ResourceLocation> added = after.stream().filter(id -> !before.contains(id)).toList();
        for (ResourceLocation id : removed) {
            Trait trait = data.trait(id);
            if (trait == null) {
                continue;
            }
            List<TraitEffect> effects = trait.effects();
            for (int i = 0; i < effects.size(); i++) {
                effects.get(i).remove(player, new EffectContext(id, i, new HashSet<>()));
            }
        }
        refresh(player);

        if (!previousOccupation.equals(occupation)) {
            NeoForge.EVENT_BUS.post(new OccupationChangeEvent(player, previousOccupation, occupation, cause));
        }
        boolean notify = cause.notifiesPlayer() && FacetsConfig.NOTIFY_CHANGES.get();
        for (ResourceLocation id : removed) {
            NeoForge.EVENT_BUS.post(new TraitChangeEvent.Removed(player, id, cause));
            if (notify) {
                send(player, new TraitNotifyPayload(id, false));
            }
        }
        for (ResourceLocation id : added) {
            NeoForge.EVENT_BUS.post(new TraitChangeEvent.Added(player, id, cause));
            if (notify) {
                send(player, new TraitNotifyPayload(id, true));
            }
        }

        // One-time commands: a new occupation's on_select, then on_gain of every gained trait
        if (!previousOccupation.equals(occupation)) {
            occupation.ifPresent(id -> {
                Occupation newOccupation = data.occupation(id);
                if (newOccupation != null) {
                    TraitCommands.run(player, newOccupation.onSelect(), "occupation " + id);
                }
            });
        }
        for (ResourceLocation id : added) {
            Trait trait = data.trait(id);
            if (trait != null) {
                TraitCommands.run(player, trait.onGain(), "trait " + id);
            }
        }
    }

    /**
     * Gives and takes traits whose gain_when / lose_when conditions match. Skipped while the player
     * still has to pick traits, so a pending selection isn't changed under them.
     */
    public static void checkTriggers(ServerPlayer player) {
        PlayerFacets playerData = get(player);
        TraitData data = data();
        if (playerData.selecting() || data.triggeredTraits().isEmpty()) {
            return;
        }

        List<ResourceLocation> held = effectiveTraits(player);
        List<ResourceLocation> own = new ArrayList<>(playerData.traits());
        List<ResourceLocation> lost = new ArrayList<>(playerData.lostOccupationTraits());
        List<ResourceLocation> fromOccupation = heldOccupationTraits(playerData);
        boolean changed = false;
        for (TraitHolder holder : data.triggeredTraits()) {
            Trait trait = holder.value();
            boolean has = held.contains(holder.id());
            if (!has && trait.gainWhen().isPresent() && trait.gainWhen().get().test(player)) {
                // Regaining a lost occupation trait gives it back as part of the occupation
                if (!lost.remove(holder.id())) {
                    own.add(holder.id());
                }
                for (TraitRef replaced : trait.replaces()) {
                    own.removeIf(id -> matches(data, replaced, id));
                    for (ResourceLocation id : fromOccupation) {
                        Trait other = data.trait(id);
                        if (other != null && other.removableFromOccupation() && matches(data, replaced, id) && !lost.contains(id)) {
                            lost.add(id);
                        }
                    }
                }
                changed = true;
            } else if (has && trait.loseWhen().isPresent() && trait.loseWhen().get().test(player)) {
                if (own.remove(holder.id())) {
                    changed = true;
                } else if (trait.removableFromOccupation() && fromOccupation.contains(holder.id()) && !lost.contains(holder.id())) {
                    lost.add(holder.id());
                    changed = true;
                }
            }
        }
        if (changed) {
            setState(player, own, playerData.occupation(), lost, ChangeCause.TRIGGER);
        }
    }

    private static boolean matches(TraitData data, TraitRef ref, ResourceLocation id) {
        TraitHolder holder = data.holder(id);
        return holder != null && ref.matches(holder);
    }

    /** Every currently active effect of the given type across the player's traits, conditions included. */
    public static <T extends TraitEffect> List<T> activeEffects(ServerPlayer player, Class<T> type) {
        TraitData data = data();
        List<T> result = new ArrayList<>();
        for (ResourceLocation id : effectiveTraits(player)) {
            Trait trait = data.trait(id);
            if (trait != null) {
                trait.effects().forEach(effect -> effect.forEachActive(player, found -> {
                    if (type.isInstance(found)) {
                        result.add(type.cast(found));
                    }
                }));
            }
        }
        return result;
    }

    /** Whether any of the player's effects has to be reapplied regularly. */
    public static boolean needsTicking(ServerPlayer player) {
        TraitData data = data();
        for (ResourceLocation id : effectiveTraits(player)) {
            Trait trait = data.trait(id);
            if (trait != null && trait.effects().stream().anyMatch(TraitEffect::needsTicking)) {
                return true;
            }
        }
        return false;
    }

    /** Reapplies every effect of the player's traits and removes leftovers from traits they no longer hold. */
    public static void refresh(ServerPlayer player) {
        refresh(player, true);
    }

    /**
     * @param sync whether to send the player's traits to their client; not needed for the periodic
     *             refresh of conditional effects, since the traits themselves didn't change
     */
    public static void refresh(ServerPlayer player, boolean sync) {
        TraitData data = data();
        Set<ResourceLocation> applied = new HashSet<>();

        List<ResourceLocation> held = effectiveTraits(player);
        for (ResourceLocation id : held) {
            Trait trait = data.trait(id);
            if (trait == null) {
                if (sync) {
                    Facets.LOGGER.warn("Player {} has unknown trait {}, ignoring it", player.getGameProfile().getName(), id);
                }
                continue;
            }
            List<TraitEffect> effects = trait.effects();
            for (int i = 0; i < effects.size(); i++) {
                effects.get(i).apply(player, new EffectContext(id, i, applied));
            }
        }

        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) {
                return;
            }
            for (AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
                if (EffectContext.isTraitModifier(modifier.id()) && !applied.contains(modifier.id())) {
                    instance.removeModifier(modifier.id());
                }
            }
        });

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        if (sync) {
            send(player, new SyncTraitsPayload(get(player).occupation(), held));
        }
    }

    /** Sends the loaded traits, categories and occupations to the player's client. */
    public static void syncData(ServerPlayer player) {
        send(player, new SyncDataPayload(data()));
    }

    public static void onLogin(ServerPlayer player) {
        PlayerFacets playerData = get(player);
        if (!playerData.initialized()) {
            playerData = playerData.withInitialized(true).withSelecting(FacetsConfig.OPEN_ON_FIRST_JOIN.get());
            set(player, playerData);
            // A new player is a new character
            snapshotStats(player);
            playerData = get(player);
        }
        refresh(player);
        if (playerData.selecting()) {
            openSelection(player);
        }
    }

    public static void onRespawn(ServerPlayer player, boolean endConquered) {
        if (endConquered) {
            refresh(player);
            return;
        }

        switch (FacetsConfig.ON_DEATH.get()) {
            case KEEP -> refresh(player);
            case RESET -> {
                resetCharacter(player);
                set(player, get(player).withSelecting(true));
            }
            case REPICK -> {
                refresh(player);
                set(player, get(player).withSelecting(true));
            }
        }
        // Respawning resets health to 20, top it up in case traits raised max health
        player.setHealth(player.getMaxHealth());

        if (get(player).selecting()) {
            openSelection(player);
        }
    }

    /**
     * Marks the player as owing a selection and opens the screen, unless selection is disabled or
     * there's nothing to pick.
     */
    public static OpenResult openSelection(ServerPlayer player) {
        SelectionRules rules = SelectionRules.fromConfig();
        TraitData data = data();
        if (rules.mode() == SelectionMode.NONE) {
            set(player, get(player).withSelecting(false));
            return OpenResult.DISABLED;
        }
        boolean anythingToPick = data.holders().stream().anyMatch(t -> t.value().selectable()) || data.occupationCount() > 0;
        if (!anythingToPick) {
            // Kept as owed, so the screen opens once traits are added and the player rejoins or /reload runs
            set(player, get(player).withSelecting(true));
            return OpenResult.NOTHING_TO_PICK;
        }
        PlayerFacets playerData = get(player).withSelecting(true);
        set(player, playerData);

        List<ResourceLocation> granted = grantedTraits(player);
        List<ResourceLocation> current = playerData.traits().stream().filter(id -> !granted.contains(id)).toList();
        send(player, new OpenSelectionPayload(rules, current, playerData.occupation(), granted));
        return OpenResult.OPENED;
    }

    /** After a reload, opens the screen for players still owing a selection, e.g. because nothing was loaded before. */
    public static void onReload(ServerPlayer player) {
        refresh(player);
        if (get(player).selecting()) {
            openSelection(player);
        }
    }

    /** Handles a selection sent from the screen. */
    public static void submit(ServerPlayer player, @org.jetbrains.annotations.Nullable ResourceLocation occupation,
                              List<ResourceLocation> requested) {
        PlayerFacets playerData = get(player);
        if (!playerData.selecting()) {
            reply(player, false, Component.translatable("facets.error.not_selecting"));
            return;
        }

        List<ResourceLocation> granted = grantedTraits(player);
        SelectionContext context = new SelectionContext(data(), granted);
        Optional<Component> error = SelectionRules.fromConfig().validate(requested, occupation, context);
        if (error.isPresent()) {
            reply(player, false, error.get());
            return;
        }

        // Traits that can't be picked on the screen are kept
        List<ResourceLocation> result = new ArrayList<>(granted);
        for (ResourceLocation id : requested) {
            if (!result.contains(id)) {
                result.add(id);
            }
        }

        setState(player, result, Optional.ofNullable(occupation), ChangeCause.SELECTION);
        set(player, get(player).withSelecting(false));
        reply(player, true, CommonComponents.EMPTY);
    }

    private static void reply(ServerPlayer player, boolean accepted, Component message) {
        send(player, new SelectionResultPayload(accepted, message));
    }

    /** Skips players whose client can't receive the payload instead of throwing. */
    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        // Fake players (machines, automation) have no real connection to send to
        if (player instanceof FakePlayer) {
            return;
        }
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
