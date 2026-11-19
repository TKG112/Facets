package net.tkg.facets.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

/**
 * A group of traits with its own list on the selection screen, defined in
 * data/&lt;namespace&gt;/facets/category/&lt;name&gt;.json
 *
 * @param name  label shown above the list
 * @param color color of the label and of the cost numbers in this list
 * @param order lists are shown from lowest to highest order
 * @param min   minimum number of traits a player has to pick from this category
 * @param max   maximum number of traits a player can pick from this category
 * @param sort  order of the traits in the list; defaults to cost in POINTS mode and name otherwise
 */
public record TraitCategory(Component name, Optional<TextColor> color, int order, int min, Optional<Integer> max,
                            Optional<SortOrder> sort) {

    public static final Codec<TraitCategory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(TraitCategory::name),
            TextColor.CODEC.optionalFieldOf("color").forGetter(TraitCategory::color),
            Codec.INT.optionalFieldOf("order", 0).forGetter(TraitCategory::order),
            Codec.INT.optionalFieldOf("min", 0).forGetter(TraitCategory::min),
            Codec.INT.optionalFieldOf("max").forGetter(TraitCategory::max),
            SortOrder.CODEC.optionalFieldOf("sort").forGetter(TraitCategory::sort)
    ).apply(instance, TraitCategory::new));
}
