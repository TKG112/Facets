package net.tkg.facets.client.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.tkg.facets.Facets;

import java.io.Reader;
import java.util.Optional;

/** Loads assets/facets/theme.json from the top-most resource pack that has one. */
public class ThemeLoader extends SimplePreparableReloadListener<FacetsTheme> {
    private static final ResourceLocation LOCATION = Facets.id("theme.json");

    @Override
    protected FacetsTheme prepare(ResourceManager manager, ProfilerFiller profiler) {
        Optional<Resource> resource = manager.getResource(LOCATION);
        if (resource.isEmpty()) {
            return FacetsTheme.DEFAULT;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            return FacetsTheme.CODEC.parse(JsonOps.INSTANCE, json)
                    .ifError(error -> Facets.LOGGER.error("Couldn't load theme from pack {}: {}",
                            resource.get().sourcePackId(), error.message()))
                    .result()
                    .map(theme -> {
                        theme.colors().keySet().stream().filter(key -> !FacetsTheme.isKnownColor(key))
                                .forEach(key -> Facets.LOGGER.warn("Unknown theme color \"{}\" in pack {}", key,
                                        resource.get().sourcePackId()));
                        return theme.withDefaults();
                    })
                    .orElse(FacetsTheme.DEFAULT);
        } catch (Exception e) {
            Facets.LOGGER.error("Couldn't read theme from pack {}", resource.get().sourcePackId(), e);
            return FacetsTheme.DEFAULT;
        }
    }

    @Override
    protected void apply(FacetsTheme theme, ResourceManager manager, ProfilerFiller profiler) {
        FacetsTheme.set(theme);
    }
}
