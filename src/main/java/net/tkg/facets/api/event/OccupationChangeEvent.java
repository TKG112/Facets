package net.tkg.facets.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.tkg.facets.api.ChangeCause;

import java.util.Optional;

/**
 * Fired on the NeoForge event bus after a player's occupation changed. Trait events for the
 * occupation's free traits are fired as well.
 */
public class OccupationChangeEvent extends Event {
    private final ServerPlayer player;
    private final Optional<ResourceLocation> previous;
    private final Optional<ResourceLocation> current;
    private final ChangeCause cause;

    public OccupationChangeEvent(ServerPlayer player, Optional<ResourceLocation> previous, Optional<ResourceLocation> current,
                                 ChangeCause cause) {
        this.player = player;
        this.previous = previous;
        this.current = current;
        this.cause = cause;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public Optional<ResourceLocation> getPrevious() {
        return previous;
    }

    public Optional<ResourceLocation> getCurrent() {
        return current;
    }

    public ChangeCause getCause() {
        return cause;
    }
}
