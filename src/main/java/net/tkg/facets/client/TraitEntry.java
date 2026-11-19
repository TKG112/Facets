package net.tkg.facets.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tkg.facets.trait.Trait;

public record TraitEntry(ResourceLocation id, Trait trait, ItemStack icon) {
    public TraitEntry(ResourceLocation id, Trait trait) {
        this(id, trait, new ItemStack(trait.icon()));
    }
}
