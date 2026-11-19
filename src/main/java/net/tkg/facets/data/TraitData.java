package net.tkg.facets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.trait.Keyed;
import net.tkg.facets.trait.Occupation;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitCategory;
import net.tkg.facets.trait.TraitHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything loaded from trait, category, occupation and trait tag files. Immutable; a new
 * instance replaces the old one on every reload, and the client gets a copy.
 */
public final class TraitData {
    public static final TraitData EMPTY = new TraitData(Map.of(), Map.of(), Map.of(), Map.of());

    public static final Codec<TraitData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Trait.CODEC).fieldOf("traits").forGetter(d -> d.traits),
            Codec.unboundedMap(ResourceLocation.CODEC, TraitCategory.CODEC).fieldOf("categories").forGetter(d -> d.categories),
            Codec.unboundedMap(ResourceLocation.CODEC, Occupation.CODEC).fieldOf("occupations").forGetter(d -> d.occupations),
            Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf()).fieldOf("tags").forGetter(d -> d.tags)
    ).apply(instance, TraitData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TraitData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private final Map<ResourceLocation, Trait> traits;
    private final Map<ResourceLocation, TraitCategory> categories;
    private final Map<ResourceLocation, Occupation> occupations;
    /** Trait tag id to the ids of the traits in it, nested tags already resolved. */
    private final Map<ResourceLocation, List<ResourceLocation>> tags;

    private final Map<ResourceLocation, TraitHolder> holders;
    private final List<TraitHolder> triggeredTraits;
    private final Map<ResourceLocation, TraitCategory> sortedCategories;
    private final List<Keyed<Occupation>> sortedOccupations;

    public TraitData(Map<ResourceLocation, Trait> traits, Map<ResourceLocation, TraitCategory> categories,
                     Map<ResourceLocation, Occupation> occupations, Map<ResourceLocation, List<ResourceLocation>> tags) {
        this.traits = Map.copyOf(traits);
        this.categories = Map.copyOf(categories);
        this.occupations = Map.copyOf(occupations);
        this.tags = Map.copyOf(tags);

        Map<ResourceLocation, Set<ResourceLocation>> tagsByTrait = new HashMap<>();
        this.tags.forEach((tag, members) -> members.forEach(id -> tagsByTrait.computeIfAbsent(id, k -> new HashSet<>()).add(tag)));

        Map<ResourceLocation, TraitHolder> holders = new LinkedHashMap<>();
        this.traits.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> holders.put(e.getKey(), new TraitHolder(e.getKey(), e.getValue(),
                        Set.copyOf(tagsByTrait.getOrDefault(e.getKey(), Set.of())))));
        this.holders = Collections.unmodifiableMap(holders);
        this.triggeredTraits = holders.values().stream().filter(h -> h.value().hasTriggers()).toList();

        Map<ResourceLocation, TraitCategory> sortedCategories = new LinkedHashMap<>();
        this.categories.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ResourceLocation, TraitCategory> e) -> e.getValue().order())
                        .thenComparing(Map.Entry::getKey))
                .forEach(e -> sortedCategories.put(e.getKey(), e.getValue()));
        this.sortedCategories = Collections.unmodifiableMap(sortedCategories);

        List<Keyed<Occupation>> sortedOccupations = new ArrayList<>();
        this.occupations.forEach((id, occupation) -> sortedOccupations.add(new Keyed<>(id, occupation)));
        sortedOccupations.sort(Comparator.comparingInt((Keyed<Occupation> k) -> k.value().order())
                .thenComparing(k -> k.value().name().getString()));
        this.sortedOccupations = List.copyOf(sortedOccupations);
    }

    @Nullable
    public Trait trait(ResourceLocation id) {
        return traits.get(id);
    }

    @Nullable
    public TraitHolder holder(ResourceLocation id) {
        return holders.get(id);
    }

    /** All traits, sorted by id. */
    public Collection<TraitHolder> holders() {
        return holders.values();
    }

    /** Traits with a gain_when or lose_when condition. */
    public List<TraitHolder> triggeredTraits() {
        return triggeredTraits;
    }

    public Set<ResourceLocation> traitIds() {
        return holders.keySet();
    }

    @Nullable
    public Occupation occupation(@Nullable ResourceLocation id) {
        return id == null ? null : occupations.get(id);
    }

    public Set<ResourceLocation> occupationIds() {
        return occupations.keySet();
    }

    /** Occupations sorted by order, then name. */
    public List<Keyed<Occupation>> sortedOccupations() {
        return sortedOccupations;
    }

    /** Categories sorted by order. */
    public Map<ResourceLocation, TraitCategory> sortedCategories() {
        return sortedCategories;
    }

    public int traitCount() {
        return traits.size();
    }

    public int categoryCount() {
        return categories.size();
    }

    public int occupationCount() {
        return occupations.size();
    }

    public int tagCount() {
        return tags.size();
    }
}
