package net.tkg.facets.trait;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.config.FacetsConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The rules a selection has to follow. Built from the server config and sent to the client with
 * the screen, so both sides validate the same way.
 */
public record SelectionRules(SelectionMode mode, int points, int minTraits, int maxTraits) {
    public static final StreamCodec<ByteBuf, SelectionRules> STREAM_CODEC = StreamCodec.composite(
            SelectionMode.STREAM_CODEC, SelectionRules::mode,
            ByteBufCodecs.INT, SelectionRules::points,
            ByteBufCodecs.VAR_INT, SelectionRules::minTraits,
            ByteBufCodecs.VAR_INT, SelectionRules::maxTraits,
            SelectionRules::new);

    public static SelectionRules fromConfig() {
        int min = FacetsConfig.MIN_TRAITS.get();
        return new SelectionRules(
                FacetsConfig.SELECTION_MODE.get(),
                FacetsConfig.STARTING_POINTS.get(),
                min,
                Math.max(min, FacetsConfig.MAX_TRAITS.get()));
    }

    public int remainingPoints(Collection<Trait> traits) {
        int remaining = points;
        for (Trait trait : traits) {
            remaining -= trait.cost();
        }
        return remaining;
    }

    /**
     * Checks a full selection.
     *
     * @return the reason the selection is invalid, or empty if it is valid
     */
    public Optional<Component> validate(List<ResourceLocation> ids, Function<ResourceLocation, Trait> lookup) {
        if (mode == SelectionMode.NONE) {
            return error("facets.error.disabled");
        }

        Set<ResourceLocation> seen = new HashSet<>();
        List<Trait> traits = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            if (!seen.add(id)) {
                return error("facets.error.duplicate", id.toString());
            }
            Trait trait = lookup.apply(id);
            if (trait == null) {
                return error("facets.error.unknown", id.toString());
            }
            if (!trait.selectable()) {
                return error("facets.error.not_selectable", trait.name());
            }
            traits.add(trait);
        }

        for (int i = 0; i < traits.size(); i++) {
            for (int j = i + 1; j < traits.size(); j++) {
                if (Trait.conflicts(ids.get(i), traits.get(i), ids.get(j), traits.get(j))) {
                    return error("facets.error.conflict", traits.get(i).name(), traits.get(j).name());
                }
            }
        }

        switch (mode) {
            case POINTS -> {
                int remaining = remainingPoints(traits);
                if (remaining < 0) {
                    return error("facets.error.points", remaining);
                }
            }
            case COUNT -> {
                if (traits.size() < minTraits) {
                    return error("facets.error.too_few", minTraits);
                }
                if (traits.size() > maxTraits) {
                    return error("facets.error.too_many", maxTraits);
                }
            }
            default -> {
            }
        }
        return Optional.empty();
    }

    private static Optional<Component> error(String key, Object... args) {
        return Optional.of(Component.translatable(key, args));
    }
}
