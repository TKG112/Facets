package net.tkg.facets.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * Server script events:
 * <pre>
 * FacetsEvents.traitAdded(event => { event.player.tell(`You got ${event.trait}`) })
 * FacetsEvents.traitRemoved(event => { ... })
 * FacetsEvents.occupationChanged(event => { event.previous, event.current })
 * FacetsEvents.scriptEffect(event => { if (event.id == 'mypack:glow' &amp;&amp; event.applying) ... })
 * </pre>
 */
public final class FacetsJSEvents {
    public static final EventGroup GROUP = EventGroup.of("FacetsEvents");

    public static final EventHandler TRAIT_ADDED = GROUP.server("traitAdded", () -> TraitChangedKubeEvent.class);
    public static final EventHandler TRAIT_REMOVED = GROUP.server("traitRemoved", () -> TraitChangedKubeEvent.class);
    public static final EventHandler OCCUPATION_CHANGED = GROUP.server("occupationChanged", () -> OccupationChangedKubeEvent.class);
    public static final EventHandler SCRIPT_EFFECT = GROUP.server("scriptEffect", () -> ScriptEffectKubeEvent.class);

    private FacetsJSEvents() {
    }
}
