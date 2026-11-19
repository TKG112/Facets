package net.tkg.facets.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.data.TraitData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Read access to traits, categories and occupations plus the player's granted traits, used to
 * validate a selection the same way on the server and on the client.
 */
public final class SelectionContext {
    private final TraitData data;
    private final List<TraitHolder> granted;

    /**
     * @param grantedIds traits the player already holds outside the selection (from commands or other
     *                   mods); they count for "requires" and "exclusive_with" checks
     */
    public SelectionContext(TraitData data, List<ResourceLocation> grantedIds) {
        this.data = data;
        this.granted = grantedIds.stream().map(data::holder).filter(Objects::nonNull).toList();
    }

    @Nullable
    public TraitHolder trait(ResourceLocation id) {
        return data.holder(id);
    }

    public Collection<TraitHolder> allTraits() {
        return data.holders();
    }

    @Nullable
    public Occupation occupation(@Nullable ResourceLocation id) {
        return data.occupation(id);
    }

    public boolean hasOccupations() {
        return data.occupationCount() > 0;
    }

    /** Occupations sorted by order, then name. */
    public List<Keyed<Occupation>> allOccupations() {
        return data.sortedOccupations();
    }

    /** Categories sorted by order. */
    public Map<ResourceLocation, TraitCategory> categories() {
        return data.sortedCategories();
    }

    public List<TraitHolder> granted() {
        return granted;
    }

    /** Free traits given by an occupation. */
    public List<TraitHolder> freeTraits(@Nullable Occupation occupation) {
        if (occupation == null) {
            return List.of();
        }
        return occupation.traits().stream().map(data::holder).filter(Objects::nonNull).toList();
    }

    public Component refName(TraitRef ref) {
        if (!ref.tag()) {
            TraitHolder trait = trait(ref.location());
            if (trait != null) {
                return trait.value().name();
            }
        }
        return Component.literal(ref.toString());
    }
}
