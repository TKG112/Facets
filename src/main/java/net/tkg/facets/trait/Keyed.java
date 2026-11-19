package net.tkg.facets.trait;

import net.minecraft.resources.ResourceLocation;

/** A loaded value with its id. */
public record Keyed<T>(ResourceLocation id, T value) {
}
