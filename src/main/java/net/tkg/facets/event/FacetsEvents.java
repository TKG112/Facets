package net.tkg.facets.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.tkg.facets.Facets;
import net.tkg.facets.command.FacetsCommand;
import net.tkg.facets.trait.TraitManager;

@EventBusSubscriber(modid = Facets.MODID)
public final class FacetsEvents {
    private FacetsEvents() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TraitManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TraitManager.onRespawn(player, event.isEndConquered());
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // A null player means /reload; joining players are handled on login
        if (event.getPlayer() == null) {
            event.getRelevantPlayers().forEach(TraitManager::refresh);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FacetsCommand.register(event.getDispatcher());
    }
}
