package net.tkg.facets.trait;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.config.FacetsConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /** Starting points plus the occupation's bonus. */
    public int budget(@Nullable Occupation occupation) {
        return points + (occupation == null ? 0 : occupation.points());
    }

    public int remainingPoints(@Nullable Occupation occupation, Collection<Trait> traits) {
        int remaining = budget(occupation);
        for (Trait trait : traits) {
            remaining -= trait.cost();
        }
        return remaining;
    }

    /**
     * Checks a full selection.
     *
     * @param ids          traits picked on the screen, not including the occupation's free traits
     * @param occupationId picked occupation, null if none
     * @return the reason the selection is invalid, or empty if it is valid
     */
    public Optional<Component> validate(List<ResourceLocation> ids, @Nullable ResourceLocation occupationId, SelectionContext context) {
        if (mode == SelectionMode.NONE) {
            return error("facets.error.disabled");
        }

        Occupation occupation = null;
        if (occupationId != null) {
            occupation = context.occupation(occupationId);
            if (occupation == null) {
                return error("facets.error.unknown_occupation", occupationId.toString());
            }
        } else if (context.hasOccupations()) {
            return error("facets.error.no_occupation");
        }
        List<TraitHolder> free = context.freeTraits(occupation);

        Set<ResourceLocation> seen = new HashSet<>();
        List<TraitHolder> picked = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            if (!seen.add(id)) {
                return error("facets.error.duplicate", id.toString());
            }
            TraitHolder trait = context.trait(id);
            if (trait == null) {
                return error("facets.error.unknown", id.toString());
            }
            if (!trait.value().selectable()) {
                return error("facets.error.not_selectable", trait.value().name());
            }
            if (free.contains(trait) || !trait.value().availableFor(trait, occupationId, occupation)) {
                return error("facets.error.not_available", trait.value().name());
            }
            picked.add(trait);
        }

        List<TraitHolder> held = new ArrayList<>(picked);
        held.addAll(free);
        held.addAll(context.granted());

        for (TraitHolder trait : picked) {
            for (TraitHolder other : held) {
                if (other != trait && Trait.conflicts(trait, other)) {
                    return error("facets.error.conflict", trait.value().name(), other.value().name());
                }
            }
            for (TraitRef required : trait.value().requires()) {
                if (held.stream().noneMatch(required::matches)) {
                    return error("facets.error.requires", trait.value().name(), context.refName(required));
                }
            }
        }

        switch (mode) {
            case POINTS -> {
                int remaining = remainingPoints(occupation, picked.stream().map(TraitHolder::value).toList());
                if (remaining < 0) {
                    return error("facets.error.points", remaining);
                }
            }
            case COUNT -> {
                if (picked.size() < minTraits) {
                    return error("facets.error.too_few", minTraits);
                }
                if (picked.size() > maxTraits) {
                    return error("facets.error.too_many", maxTraits);
                }
            }
            default -> {
            }
        }

        for (Map.Entry<ResourceLocation, TraitCategory> entry : context.categories().entrySet()) {
            TraitCategory category = entry.getValue();
            long count = picked.stream().filter(t -> t.value().category().filter(entry.getKey()::equals).isPresent()).count();
            if (count < category.min()) {
                return error("facets.error.category_too_few", category.min(), category.name());
            }
            if (category.max().isPresent() && count > category.max().get()) {
                return error("facets.error.category_too_many", category.max().get(), category.name());
            }
        }
        return Optional.empty();
    }

    private static Optional<Component> error(String key, Object... args) {
        return Optional.of(Component.translatable(key, args));
    }
}
