package net.tkg.facets.trait;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;

/**
 * Points at a single trait ("mypack:strong") or a trait tag ("#mypack:strength").
 * Tags live in data/&lt;namespace&gt;/tags/facets/trait/&lt;name&gt;.json
 */
public record TraitRef(ResourceLocation location, boolean tag) {
    public static final Codec<TraitRef> CODEC = Codec.STRING.comapFlatMap(TraitRef::parse, TraitRef::toString);

    private static DataResult<TraitRef> parse(String value) {
        boolean tag = value.startsWith("#");
        return ResourceLocation.read(tag ? value.substring(1) : value).map(location -> new TraitRef(location, tag));
    }

    public boolean matches(TraitHolder trait) {
        return trait.is(this);
    }

    @Override
    public String toString() {
        return (tag ? "#" : "") + location;
    }
}
