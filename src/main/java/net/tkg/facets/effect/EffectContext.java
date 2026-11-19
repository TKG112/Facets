package net.tkg.facets.effect;

import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;

import java.util.Set;

/**
 * @param traitId          the trait the effect belongs to
 * @param index            position of the effect inside the trait's effect list
 * @param appliedModifiers attribute modifier ids applied during this refresh; anything else owned by
 *                         Facets is removed afterwards
 */
public record EffectContext(ResourceLocation traitId, int index, Set<ResourceLocation> appliedModifiers) {
    private static final String MODIFIER_PREFIX = "trait/";

    public ResourceLocation modifierId() {
        return Facets.id(MODIFIER_PREFIX + traitId.getNamespace() + "/" + traitId.getPath() + "/" + index);
    }

    public static boolean isTraitModifier(ResourceLocation id) {
        return id.getNamespace().equals(Facets.MODID) && id.getPath().startsWith(MODIFIER_PREFIX);
    }
}
