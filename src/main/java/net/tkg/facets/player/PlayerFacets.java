package net.tkg.facets.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Per-player trait data.
 *
 * @param traits      ids of the traits the player holds, not including the occupation's free traits
 * @param occupation  the player's occupation, if any
 * @param selecting   the player still owes a selection; the screen reopens on login until it's done
 * @param initialized false until the player's first login was handled
 */
public record PlayerFacets(List<ResourceLocation> traits, Optional<ResourceLocation> occupation, boolean selecting,
                           boolean initialized) {
    public static final PlayerFacets DEFAULT = new PlayerFacets(List.of(), Optional.empty(), false, false);

    public static final Codec<PlayerFacets> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(PlayerFacets::traits),
            ResourceLocation.CODEC.optionalFieldOf("occupation").forGetter(PlayerFacets::occupation),
            Codec.BOOL.optionalFieldOf("selecting", false).forGetter(PlayerFacets::selecting),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(PlayerFacets::initialized)
    ).apply(instance, PlayerFacets::new));

    public PlayerFacets withTraits(List<ResourceLocation> traits) {
        return new PlayerFacets(List.copyOf(traits), occupation, selecting, initialized);
    }

    public PlayerFacets withOccupation(Optional<ResourceLocation> occupation) {
        return new PlayerFacets(traits, occupation, selecting, initialized);
    }

    public PlayerFacets withSelecting(boolean selecting) {
        return new PlayerFacets(traits, occupation, selecting, initialized);
    }

    public PlayerFacets withInitialized(boolean initialized) {
        return new PlayerFacets(traits, occupation, selecting, initialized);
    }
}
