package net.tkg.facets.compat.kubejs;

import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tkg.facets.api.ChangeCause;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/** event.player, event.previous and event.current (occupation ids, or null), event.cause */
public class OccupationChangedKubeEvent implements KubePlayerEvent {
    private final ServerPlayer player;
    private final Optional<ResourceLocation> previous;
    private final Optional<ResourceLocation> current;
    private final ChangeCause cause;

    public OccupationChangedKubeEvent(ServerPlayer player, Optional<ResourceLocation> previous,
                                      Optional<ResourceLocation> current, ChangeCause cause) {
        this.player = player;
        this.previous = previous;
        this.current = current;
        this.cause = cause;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    @Nullable
    public String getPrevious() {
        return previous.map(ResourceLocation::toString).orElse(null);
    }

    @Nullable
    public String getCurrent() {
        return current.map(ResourceLocation::toString).orElse(null);
    }

    public String getCause() {
        return cause.name().toLowerCase(Locale.ROOT);
    }
}
