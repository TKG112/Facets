package net.tkg.facets.trait;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tkg.facets.Facets;
import net.tkg.facets.config.FacetsConfig;
import net.tkg.facets.effect.EffectContext;
import net.tkg.facets.effect.TraitEffect;
import net.tkg.facets.network.OpenSelectionPayload;
import net.tkg.facets.network.SelectionResultPayload;
import net.tkg.facets.player.FacetsAttachments;
import net.tkg.facets.player.PlayerFacets;
import net.tkg.facets.registry.FacetsRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Server-side entry point for reading and changing a player's traits. */
public final class TraitManager {
    private TraitManager() {
    }

    public static Registry<Trait> traits(MinecraftServer server) {
        return server.registryAccess().registryOrThrow(FacetsRegistries.TRAIT);
    }

    public static PlayerFacets get(ServerPlayer player) {
        return player.getData(FacetsAttachments.PLAYER_FACETS);
    }

    private static void set(ServerPlayer player, PlayerFacets data) {
        player.setData(FacetsAttachments.PLAYER_FACETS, data);
    }

    /** Replaces the player's traits, ignoring selection rules. */
    public static void setTraits(ServerPlayer player, List<ResourceLocation> traits) {
        PlayerFacets old = get(player);
        Registry<Trait> registry = traits(player.server);
        for (ResourceLocation id : old.traits()) {
            if (traits.contains(id)) {
                continue;
            }
            Trait trait = registry.get(id);
            if (trait == null) {
                continue;
            }
            List<TraitEffect> effects = trait.effects();
            for (int i = 0; i < effects.size(); i++) {
                effects.get(i).remove(player, new EffectContext(id, i, new HashSet<>()));
            }
        }
        set(player, old.withTraits(traits));
        refresh(player);
    }

    /** Reapplies every effect of the player's traits and removes leftovers from traits they no longer hold. */
    public static void refresh(ServerPlayer player) {
        Registry<Trait> registry = traits(player.server);
        Set<ResourceLocation> applied = new HashSet<>();

        for (ResourceLocation id : get(player).traits()) {
            Trait trait = registry.get(id);
            if (trait == null) {
                Facets.LOGGER.warn("Player {} has unknown trait {}, ignoring it", player.getGameProfile().getName(), id);
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
    }

    public static void onLogin(ServerPlayer player) {
        PlayerFacets data = get(player);
        if (!data.initialized()) {
            data = data.withInitialized(true).withSelecting(FacetsConfig.OPEN_ON_FIRST_JOIN.get());
            set(player, data);
        }
        refresh(player);
        if (data.selecting()) {
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
                setTraits(player, List.of());
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
     * Marks the player as owing a selection and opens the screen. Does nothing if selection is
     * disabled or there's nothing to pick.
     *
     * @return whether the screen was opened
     */
    public static boolean openSelection(ServerPlayer player) {
        SelectionRules rules = SelectionRules.fromConfig();
        boolean anySelectable = traits(player.server).stream().anyMatch(Trait::selectable);
        if (rules.mode() == SelectionMode.NONE || !anySelectable) {
            set(player, get(player).withSelecting(false));
            return false;
        }
        PlayerFacets data = get(player).withSelecting(true);
        set(player, data);
        send(player, new OpenSelectionPayload(rules, data.traits()));
        return true;
    }

    /** Handles a selection sent from the screen. */
    public static void submit(ServerPlayer player, List<ResourceLocation> requested) {
        PlayerFacets data = get(player);
        if (!data.selecting()) {
            reply(player, false, Component.translatable("facets.error.not_selecting"));
            return;
        }

        Registry<Trait> registry = traits(player.server);
        Optional<Component> error = SelectionRules.fromConfig().validate(requested, registry::get);
        if (error.isPresent()) {
            reply(player, false, error.get());
            return;
        }

        // Traits that can't be picked on the screen (granted by commands or other mods) are kept
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : data.traits()) {
            Trait trait = registry.get(id);
            if (trait == null || !trait.selectable()) {
                result.add(id);
            }
        }
        for (ResourceLocation id : requested) {
            if (!result.contains(id)) {
                result.add(id);
            }
        }

        setTraits(player, result);
        set(player, get(player).withSelecting(false));
        reply(player, true, CommonComponents.EMPTY);
    }

    private static void reply(ServerPlayer player, boolean accepted, Component message) {
        send(player, new SelectionResultPayload(accepted, message));
    }

    /** Skips players whose client can't receive the payload instead of throwing. */
    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
