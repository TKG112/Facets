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
import net.tkg.facets.effect.TraitEffect;

import java.util.List;

/**
 * A trait as defined in data/&lt;namespace&gt;/facets/trait/&lt;name&gt;.json
 *
 * @param name          display name, a text component (plain string or {"translate": ...})
 * @param description   shown in the tooltip on the selection screen
 * @param icon          item shown next to the name
 * @param cost          points spent when taken (negative values give points back), only used in POINTS mode
 * @param selectable    false hides the trait from the screen; it can then only be granted by commands or other mods
 * @param exclusiveWith traits that can't be held together with this one (checked both ways)
 * @param effects       what the trait does
 */
public record Trait(Component name, Component description, Item icon, int cost, boolean selectable,
                    List<ResourceLocation> exclusiveWith, List<TraitEffect> effects) {

    public static final Codec<Trait> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Trait::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", CommonComponents.EMPTY).forGetter(Trait::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PAPER).forGetter(Trait::icon),
            Codec.INT.optionalFieldOf("cost", 0).forGetter(Trait::cost),
            Codec.BOOL.optionalFieldOf("selectable", true).forGetter(Trait::selectable),
            ResourceLocation.CODEC.listOf().optionalFieldOf("exclusive_with", List.of()).forGetter(Trait::exclusiveWith),
            TraitEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Trait::effects)
    ).apply(instance, Trait::new));

    public static boolean conflicts(ResourceLocation aId, Trait a, ResourceLocation bId, Trait b) {
        return a.exclusiveWith().contains(bId) || b.exclusiveWith().contains(aId);
    }
}
