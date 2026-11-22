package net.tkg.facets.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Per-player trait data.
 *
 * @param traits               ids of the traits the player holds, not including the occupation's free traits
 * @param occupation           the player's occupation, if any
 * @param lostOccupationTraits free traits of the occupation the player lost during play (removable_from_occupation)
 * @param statBaseline         statistic values when the current character started, for stat conditions with
 *                             "since": "character"
 * @param selecting            the player still owes a selection; the screen reopens on login until it's done
 * @param initialized          false until the player's first login was handled
 */
public record PlayerFacets(List<ResourceLocation> traits, Optional<ResourceLocation> occupation,
                           List<ResourceLocation> lostOccupationTraits, Map<String, Integer> statBaseline,
                           boolean selecting, boolean initialized) {
    public static final PlayerFacets DEFAULT = new PlayerFacets(List.of(), Optional.empty(), List.of(), Map.of(), false, false);

    public static final Codec<PlayerFacets> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("traits", List.of()).forGetter(PlayerFacets::traits),
            ResourceLocation.CODEC.optionalFieldOf("occupation").forGetter(PlayerFacets::occupation),
            ResourceLocation.CODEC.listOf().optionalFieldOf("lost_occupation_traits", List.of()).forGetter(PlayerFacets::lostOccupationTraits),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("stat_baseline", Map.of()).forGetter(PlayerFacets::statBaseline),
            Codec.BOOL.optionalFieldOf("selecting", false).forGetter(PlayerFacets::selecting),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(PlayerFacets::initialized)
    ).apply(instance, PlayerFacets::new));

    public PlayerFacets withTraits(List<ResourceLocation> traits) {
        return new PlayerFacets(List.copyOf(traits), occupation, lostOccupationTraits, statBaseline, selecting, initialized);
    }

    public PlayerFacets withOccupation(Optional<ResourceLocation> occupation) {
        return new PlayerFacets(traits, occupation, lostOccupationTraits, statBaseline, selecting, initialized);
    }

    public PlayerFacets withLostOccupationTraits(List<ResourceLocation> lost) {
        return new PlayerFacets(traits, occupation, List.copyOf(lost), statBaseline, selecting, initialized);
    }

    public PlayerFacets withStatBaseline(Map<String, Integer> baseline) {
        return new PlayerFacets(traits, occupation, lostOccupationTraits, Map.copyOf(baseline), selecting, initialized);
    }

    public PlayerFacets withSelecting(boolean selecting) {
        return new PlayerFacets(traits, occupation, lostOccupationTraits, statBaseline, selecting, initialized);
    }

    public PlayerFacets withInitialized(boolean initialized) {
        return new PlayerFacets(traits, occupation, lostOccupationTraits, statBaseline, selecting, initialized);
    }
}
