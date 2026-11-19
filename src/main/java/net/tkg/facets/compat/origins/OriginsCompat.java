package net.tkg.facets.compat.origins;

import com.cyberday1.neoorigins.api.NeoOriginsAPI;
import com.cyberday1.neoorigins.api.power.GrantPowerResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.facets.Facets;
import net.tkg.facets.condition.EffectCondition;
import net.tkg.facets.effect.EffectContext;
import net.tkg.facets.effect.TraitEffect;
import net.tkg.facets.registry.FacetsRegistries;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** NeoOrigins integration. Only loaded when neoorigins is installed. */
public final class OriginsCompat {
    public static final String MOD_ID = "neoorigins";

    private static final DeferredRegister<MapCodec<? extends TraitEffect>> EFFECTS =
            DeferredRegister.create(FacetsRegistries.EFFECT_TYPE, Facets.MODID);
    private static final DeferredRegister<MapCodec<? extends EffectCondition>> CONDITIONS =
            DeferredRegister.create(FacetsRegistries.CONDITION_TYPE, Facets.MODID);

    /** Missing powers are logged once per id, not on every refresh. */
    private static final Set<ResourceLocation> WARNED = new HashSet<>();

    static {
        EFFECTS.register("origins_power", () -> GrantPower.CODEC);
        CONDITIONS.register("origin", () -> HasOrigin.CODEC);
    }

    private OriginsCompat() {
    }

    public static void init(IEventBus modBus) {
        EFFECTS.register(modBus);
        CONDITIONS.register(modBus);
    }

    /**
     * Grants an Origins power while the trait is held.
     * <pre>
     * { "type": "facets:origins_power", "power": "origins:fire_immunity" }
     * </pre>
     * Each trait grants under its own source, so two traits granting the same power don't take it
     * from each other, and the player's origin or other mods can't remove it either.
     */
    public record GrantPower(ResourceLocation power) implements TraitEffect {
        public static final MapCodec<GrantPower> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("power").forGetter(GrantPower::power)
        ).apply(i, GrantPower::new));

        @Override
        public MapCodec<? extends TraitEffect> codec() {
            return CODEC;
        }

        private static ResourceLocation source(EffectContext context) {
            ResourceLocation trait = context.traitId();
            return Facets.id("trait/" + trait.getNamespace() + "/" + trait.getPath());
        }

        @Override
        public void apply(ServerPlayer player, EffectContext context) {
            GrantPowerResult result = NeoOriginsAPI.grantPower(player, power, source(context));
            if (result == GrantPowerResult.UNKNOWN_POWER && WARNED.add(power)) {
                Facets.LOGGER.warn("Trait {} grants unknown Origins power {}", context.traitId(), power);
            }
        }

        @Override
        public void remove(ServerPlayer player, EffectContext context) {
            NeoOriginsAPI.revokePower(player, power, source(context));
        }

        @Override
        public void addTooltip(Consumer<Component> lines) {
            Component name = Component.translatableWithFallback(
                    "power." + power.getNamespace() + "." + power.getPath() + ".name", power.toString());
            lines.accept(Component.translatable("facets.effect.origins_power", name).withStyle(ChatFormatting.BLUE));
        }
    }

    /** { "type": "facets:origin", "origin": "origins:blazeborn", "layer": "origins:origin" } (layer optional) */
    public record HasOrigin(ResourceLocation origin, Optional<ResourceLocation> layer) implements EffectCondition {
        public static final MapCodec<HasOrigin> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("origin").forGetter(HasOrigin::origin),
                ResourceLocation.CODEC.optionalFieldOf("layer").forGetter(HasOrigin::layer)
        ).apply(i, HasOrigin::new));

        @Override
        public MapCodec<? extends EffectCondition> codec() {
            return CODEC;
        }

        @Override
        public boolean test(ServerPlayer player) {
            return layer.map(l -> NeoOriginsAPI.getOrigin(player, l).filter(origin::equals).isPresent())
                    .orElseGet(() -> NeoOriginsAPI.getOrigins(player).containsValue(origin));
        }

        @Override
        public Component describe() {
            return Component.translatable("facets.condition.origin", Component.translatableWithFallback(
                    "origin." + origin.getNamespace() + "." + origin.getPath() + ".name", origin.toString()));
        }
    }
}
