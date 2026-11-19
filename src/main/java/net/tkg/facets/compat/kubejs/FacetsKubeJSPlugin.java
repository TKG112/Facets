package net.tkg.facets.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import net.tkg.facets.api.FacetsApi;

/** Registered through kubejs.plugins.txt, so KubeJS only loads it when it's installed. */
public class FacetsKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(FacetsJSEvents.GROUP);
    }

    /** Facets.addTrait(player, 'mypack:strong'), Facets.hasTrait(...) and the rest of FacetsApi. */
    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("Facets", FacetsApi.class);
    }
}
