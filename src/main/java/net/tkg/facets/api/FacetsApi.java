package net.tkg.facets.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.data.TraitData;
import net.tkg.facets.trait.Occupation;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Entry point for other mods. Server side only; every change fires the events in
 * {@code net.tkg.facets.api.event} and shows the player a toast.
 * <p>
 * New effect and condition types are registered with a DeferredRegister on
 * {@code FacetsRegistries.EFFECT_TYPE} / {@code FacetsRegistries.CONDITION_TYPE}.
 */
public final class FacetsApi {
    private FacetsApi() {
    }

    /** Every trait the player holds, including the free traits of their occupation. */
    public static List<ResourceLocation> getTraits(ServerPlayer player) {
        return TraitManager.effectiveTraits(player);
    }

    public static boolean hasTrait(ServerPlayer player, ResourceLocation trait) {
        return TraitManager.effectiveTraits(player).contains(trait);
    }

    /**
     * Gives the player a trait, ignoring selection rules.
     *
     * @return false if the trait doesn't exist or the player already has it
     */
    public static boolean addTrait(ServerPlayer player, ResourceLocation trait) {
        if (getTrait(trait).isEmpty() || hasTrait(player, trait)) {
            return false;
        }
        List<ResourceLocation> traits = new ArrayList<>(TraitManager.get(player).traits());
        traits.add(trait);
        TraitManager.setTraits(player, traits, ChangeCause.API);
        return true;
    }

    /**
     * Takes a trait away from the player. Free traits of the player's occupation can only be removed
     * when the trait is marked removable_from_occupation.
     *
     * @return false if the player doesn't hold the trait, or it can't be removed
     */
    public static boolean removeTrait(ServerPlayer player, ResourceLocation trait) {
        return TraitManager.removeTrait(player, trait, ChangeCause.API);
    }

    public static Optional<ResourceLocation> getOccupation(ServerPlayer player) {
        return TraitManager.get(player).occupation();
    }

    /**
     * Sets the player's occupation, ignoring selection rules. Null clears it.
     *
     * @return false if the occupation doesn't exist
     */
    public static boolean setOccupation(ServerPlayer player, @Nullable ResourceLocation occupation) {
        if (occupation != null && getOccupationData(occupation).isEmpty()) {
            return false;
        }
        TraitManager.setState(player, TraitManager.get(player).traits(), Optional.ofNullable(occupation), ChangeCause.API);
        return true;
    }

    /** Opens the selection screen, as /facets open does. */
    public static boolean openSelection(ServerPlayer player) {
        return TraitManager.openSelection(player) == TraitManager.OpenResult.OPENED;
    }

    /** A loaded trait. Data is replaced on every reload, so don't keep the result around. */
    public static Optional<Trait> getTrait(ResourceLocation id) {
        return Optional.ofNullable(data().trait(id));
    }

    public static Optional<Occupation> getOccupationData(ResourceLocation id) {
        return Optional.ofNullable(data().occupation(id));
    }

    public static Collection<ResourceLocation> getAllTraitIds() {
        return data().traitIds();
    }

    private static TraitData data() {
        return TraitManager.data();
    }
}
