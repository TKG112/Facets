package net.tkg.facets.compat.kubejs;

import dev.latvian.mods.kubejs.player.KubePlayerEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Fired for facets:script effects. event.id is the effect's "id", event.trait the trait it belongs
 * to, event.applying is true when the effect is (re)applied and false when it's removed.
 * Applying happens on join, respawn, reload and whenever traits change, so scripts should be safe
 * to run more than once.
 */
public class ScriptEffectKubeEvent implements KubePlayerEvent {
    private final ServerPlayer player;
    private final ResourceLocation id;
    private final ResourceLocation trait;
    private final boolean applying;

    public ScriptEffectKubeEvent(ServerPlayer player, ResourceLocation id, ResourceLocation trait, boolean applying) {
        this.player = player;
        this.id = id;
        this.trait = trait;
        this.applying = applying;
    }

    @Override
    public Player getEntity() {
        return player;
    }

    public String getId() {
        return id.toString();
    }

    public String getTrait() {
        return trait.toString();
    }

    public boolean isApplying() {
        return applying;
    }
}
