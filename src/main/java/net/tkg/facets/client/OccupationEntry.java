package net.tkg.facets.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tkg.facets.trait.Keyed;
import net.tkg.facets.trait.Occupation;

public record OccupationEntry(ResourceLocation id, Occupation occupation, ItemStack icon) implements EntryListWidget.Entry {
    public OccupationEntry(Keyed<Occupation> keyed) {
        this(keyed.id(), keyed.value(), new ItemStack(keyed.value().icon()));
    }

    @Override
    public Component name() {
        return occupation.name();
    }
}
