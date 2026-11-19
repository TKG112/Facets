package net.tkg.facets.api.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.tkg.facets.api.ChangeCause;

/**
 * Fired on the NeoForge event bus after a player gains or loses a trait, including the free traits
 * of an occupation. Fired once per trait, after the trait's effects were applied or removed.
 */
public abstract class TraitChangeEvent extends Event {
    private final ServerPlayer player;
    private final ResourceLocation trait;
    private final ChangeCause cause;

    protected TraitChangeEvent(ServerPlayer player, ResourceLocation trait, ChangeCause cause) {
        this.player = player;
        this.trait = trait;
        this.cause = cause;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public ResourceLocation getTrait() {
        return trait;
    }

    public ChangeCause getCause() {
        return cause;
    }

    /** The player gained the trait. */
    public static class Added extends TraitChangeEvent {
        public Added(ServerPlayer player, ResourceLocation trait, ChangeCause cause) {
            super(player, trait, cause);
        }
    }

    /** The player lost the trait. */
    public static class Removed extends TraitChangeEvent {
        public Removed(ServerPlayer player, ResourceLocation trait, ChangeCause cause) {
            super(player, trait, cause);
        }
    }
}
