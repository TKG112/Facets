package net.tkg.facets.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.tkg.facets.Facets;
import net.tkg.facets.trait.Occupation;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitCategory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads data/&lt;namespace&gt;/facets/{trait,category,occupation}/*.json and
 * data/&lt;namespace&gt;/tags/facets/trait/*.json on every datapack (re)load.
 * A broken file is logged and skipped; it doesn't stop the world from loading.
 */
public class TraitDataLoader extends SimplePreparableReloadListener<TraitDataLoader.Raw> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String TRAITS = "facets/trait";
    private static final String CATEGORIES = "facets/category";
    private static final String OCCUPATIONS = "facets/occupation";
    private static final FileToIdConverter TAGS = FileToIdConverter.json("tags/facets/trait");

    record Raw(Map<ResourceLocation, JsonElement> traits, Map<ResourceLocation, JsonElement> categories,
               Map<ResourceLocation, JsonElement> occupations, Map<ResourceLocation, List<JsonElement>> tags) {
    }

    @Override
    protected Raw prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> traits = new HashMap<>();
        Map<ResourceLocation, JsonElement> categories = new HashMap<>();
        Map<ResourceLocation, JsonElement> occupations = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, TRAITS, GSON, traits);
        SimpleJsonResourceReloadListener.scanDirectory(manager, CATEGORIES, GSON, categories);
        SimpleJsonResourceReloadListener.scanDirectory(manager, OCCUPATIONS, GSON, occupations);

        // Every pack's copy of a tag file is kept, so tags from different packs merge like vanilla tags
        Map<ResourceLocation, List<JsonElement>> tags = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<Resource>> entry : TAGS.listMatchingResourceStacks(manager).entrySet()) {
            ResourceLocation id = TAGS.fileToId(entry.getKey());
            List<JsonElement> stack = new ArrayList<>();
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    stack.add(GsonHelper.fromJson(GSON, reader, JsonElement.class));
                } catch (Exception e) {
                    Facets.LOGGER.error("Couldn't read trait tag {} from pack {}", id, resource.sourcePackId(), e);
                }
            }
            tags.put(id, stack);
        }
        return new Raw(traits, categories, occupations, tags);
    }

    @Override
    protected void apply(Raw raw, ResourceManager manager, ProfilerFiller profiler) {
        ConditionalOps<JsonElement> ops = makeConditionalOps();
        int[] errors = {0};

        Map<ResourceLocation, Trait> traits = parseAll("trait", raw.traits(), Trait.CODEC, ops, errors);
        Map<ResourceLocation, TraitCategory> categories = parseAll("category", raw.categories(), TraitCategory.CODEC, ops, errors);
        Map<ResourceLocation, Occupation> occupations = parseAll("occupation", raw.occupations(), Occupation.CODEC, ops, errors);
        Map<ResourceLocation, List<ResourceLocation>> tags = resolveTags(raw.tags(), traits.keySet(), errors);

        warnUnknownReferences(traits, categories, occupations, tags);

        FacetsData.set(new TraitData(traits, categories, occupations, tags), errors[0]);
        Facets.LOGGER.info("Loaded {} traits, {} categories, {} occupations and {} trait tags{}", traits.size(), categories.size(),
                occupations.size(), tags.size(), errors[0] > 0 ? " (" + errors[0] + " files failed, see above)" : "");
    }

    private static <T> Map<ResourceLocation, T> parseAll(String kind, Map<ResourceLocation, JsonElement> files, Codec<T> codec,
                                                         ConditionalOps<JsonElement> ops, int[] errors) {
        Codec<Optional<T>> conditional = ConditionalOps.createConditionalCodec(codec);
        Map<ResourceLocation, T> result = new LinkedHashMap<>();
        files.forEach((id, json) -> {
            DataResult<Optional<T>> parsed = conditional.parse(ops, json);
            // Skip the whole file on any error; a partial result would silently drop broken effects
            parsed.ifError(error -> {
                Facets.LOGGER.error("Couldn't load {} {}: {}", kind, id, error.message());
                errors[0]++;
            }).ifSuccess(value -> value.ifPresent(v -> result.put(id, v)));
        });
        return result;
    }

    /** Builds each tag from every pack's copy (honoring "replace") and expands nested tags. */
    private static Map<ResourceLocation, List<ResourceLocation>> resolveTags(Map<ResourceLocation, List<JsonElement>> files,
                                                                             Set<ResourceLocation> traitIds, int[] errors) {
        Map<ResourceLocation, List<String>> entries = new HashMap<>();
        files.forEach((id, stack) -> {
            List<String> values = new ArrayList<>();
            for (JsonElement element : stack) {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(element, "trait tag");
                    if (GsonHelper.getAsBoolean(object, "replace", false)) {
                        values.clear();
                    }
                    for (JsonElement value : GsonHelper.getAsJsonArray(object, "values")) {
                        values.add(value.isJsonObject() ? GsonHelper.getAsString(value.getAsJsonObject(), "id") : value.getAsString());
                    }
                } catch (Exception e) {
                    Facets.LOGGER.error("Couldn't load trait tag {}: {}", id, e.getMessage());
                    errors[0]++;
                }
            }
            entries.put(id, values);
        });

        Map<ResourceLocation, List<ResourceLocation>> resolved = new HashMap<>();
        for (ResourceLocation tag : entries.keySet()) {
            Set<ResourceLocation> members = new LinkedHashSet<>();
            expand(tag, entries, traitIds, members, new HashSet<>());
            resolved.put(tag, List.copyOf(members));
        }
        return resolved;
    }

    private static void expand(ResourceLocation tag, Map<ResourceLocation, List<String>> entries, Set<ResourceLocation> traitIds,
                               Set<ResourceLocation> out, Set<ResourceLocation> visiting) {
        if (!visiting.add(tag)) {
            Facets.LOGGER.warn("Trait tag #{} includes itself", tag);
            return;
        }
        for (String value : entries.getOrDefault(tag, List.of())) {
            if (value.startsWith("#")) {
                ResourceLocation nested = ResourceLocation.tryParse(value.substring(1));
                if (nested == null || !entries.containsKey(nested)) {
                    Facets.LOGGER.warn("Trait tag #{} references unknown tag {}", tag, value);
                } else {
                    expand(nested, entries, traitIds, out, visiting);
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id == null || !traitIds.contains(id)) {
                    Facets.LOGGER.warn("Trait tag #{} references unknown trait {}", tag, value);
                } else {
                    out.add(id);
                }
            }
        }
        visiting.remove(tag);
    }

    /** Typos in ids would otherwise fail silently, so they're logged. */
    private static void warnUnknownReferences(Map<ResourceLocation, Trait> traits, Map<ResourceLocation, TraitCategory> categories,
                                              Map<ResourceLocation, Occupation> occupations, Map<ResourceLocation, List<ResourceLocation>> tags) {
        traits.forEach((id, trait) -> {
            trait.category().filter(c -> !categories.containsKey(c))
                    .ifPresent(c -> Facets.LOGGER.warn("Trait {} uses unknown category {}", id, c));
            trait.occupations().stream().filter(o -> !occupations.containsKey(o))
                    .forEach(o -> Facets.LOGGER.warn("Trait {} references unknown occupation {}", id, o));
            trait.exclusiveWith().stream().filter(ref -> ref.tag() ? !tags.containsKey(ref.location()) : !traits.containsKey(ref.location()))
                    .forEach(ref -> Facets.LOGGER.warn("Trait {} is exclusive with unknown {}", id, ref));
            trait.requires().stream().filter(ref -> ref.tag() ? !tags.containsKey(ref.location()) : !traits.containsKey(ref.location()))
                    .forEach(ref -> Facets.LOGGER.warn("Trait {} requires unknown {}", id, ref));
        });
        occupations.forEach((id, occupation) -> occupation.traits().stream().filter(t -> !traits.containsKey(t))
                .forEach(t -> Facets.LOGGER.warn("Occupation {} gives unknown trait {}", id, t)));
    }
}
