package net.tkg.facets.effect;

import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;

import java.util.Set;

/**
 * @param traitId          the trait the effect belongs to
 * @param path             position of the effect inside the trait ("0", or "1/0" for effects nested in a conditional)
 * @param appliedModifiers attribute modifier ids applied during this refresh; anything else owned by
 *                         Facets is removed afterwards
 */
public record EffectContext(ResourceLocation traitId, String path, Set<ResourceLocation> appliedModifiers) {
    private static final String MODIFIER_PREFIX = "trait/";

    public EffectContext(ResourceLocation traitId, int index, Set<ResourceLocation> appliedModifiers) {
        this(traitId, String.valueOf(index), appliedModifiers);
    }

    /** Context for an effect nested inside this one. */
    public EffectContext child(int index) {
        return new EffectContext(traitId, path + "/" + index, appliedModifiers);
    }

    public ResourceLocation modifierId() {
        return Facets.id(MODIFIER_PREFIX + traitId.getNamespace() + "/" + traitId.getPath() + "/" + path);
    }

    public static boolean isTraitModifier(ResourceLocation id) {
        return id.getNamespace().equals(Facets.MODID) && id.getPath().startsWith(MODIFIER_PREFIX);
    }
}
