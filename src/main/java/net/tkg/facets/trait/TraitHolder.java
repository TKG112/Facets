package net.tkg.facets.trait;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * A loaded trait together with its id and the trait tags it belongs to.
 * Two holders are equal when their ids are.
 */
public record TraitHolder(ResourceLocation id, Trait value, Set<ResourceLocation> tags) {
    public boolean is(TraitRef ref) {
        return ref.tag() ? tags.contains(ref.location()) : id.equals(ref.location());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TraitHolder holder && holder.id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
