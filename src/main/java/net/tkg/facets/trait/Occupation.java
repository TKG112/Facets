package net.tkg.facets.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * A preset picked before traits, defined in data/&lt;namespace&gt;/facets/occupation/&lt;name&gt;.json
 * When at least one occupation exists, every player has to pick one.
 *
 * @param name        display name
 * @param description shown next to the list
 * @param icon        item shown next to the name
 * @param points      added to the starting points in POINTS mode (can be negative)
 * @param order       occupations are listed from lowest to highest order, then by name
 * @param traits      traits given for free with the occupation; they can't be removed on the screen
 * @param blocked     traits or trait tags that can't be picked with this occupation
 * @param onSelect    commands run once when a player takes this occupation, as the server with @s as the player
 */
public record Occupation(Component name, Component description, Item icon, int points, int order,
                         List<ResourceLocation> traits, List<TraitRef> blocked, List<String> onSelect) {

    public static final Codec<Occupation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Occupation::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", CommonComponents.EMPTY).forGetter(Occupation::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PAPER).forGetter(Occupation::icon),
            Codec.INT.optionalFieldOf("points", 0).forGetter(Occupation::points),
            Codec.INT.optionalFieldOf("order", 0).forGetter(Occupation::order),
            ResourceLocation.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(Occupation::traits),
            TraitRef.CODEC.listOf().optionalFieldOf("blocked", List.of()).forGetter(Occupation::blocked),
            Codec.STRING.listOf().optionalFieldOf("on_select", List.of()).forGetter(Occupation::onSelect)
    ).apply(instance, Occupation::new));
}
