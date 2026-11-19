package net.tkg.facets.compat.kubejs;

import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tkg.facets.api.ChangeCause;

import java.util.Locale;

/** event.player, event.trait ("mypack:strong"), event.cause ("selection", "command", "reset", "trigger", "api") */
public class TraitChangedKubeEvent implements KubePlayerEvent {
    private final ServerPlayer player;
    private final ResourceLocation trait;
    private final ChangeCause cause;

    public TraitChangedKubeEvent(ServerPlayer player, ResourceLocation trait, ChangeCause cause) {
        this.player = player;
        this.trait = trait;
        this.cause = cause;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    public String getTrait() {
        return trait.toString();
    }

    public String getCause() {
        return cause.name().toLowerCase(Locale.ROOT);
    }
}
