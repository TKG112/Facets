package net.tkg.facets.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitHolder;

public record TraitEntry(ResourceLocation id, TraitHolder holder, ItemStack icon) implements EntryListWidget.Entry {
    public TraitEntry(TraitHolder holder) {
        this(holder.id(), holder, new ItemStack(holder.value().icon()));
    }

    public Trait trait() {
        return holder.value();
    }

    @Override
    public Component name() {
        return trait().name();
    }
}
