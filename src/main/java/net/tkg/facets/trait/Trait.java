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
import net.tkg.facets.condition.EffectCondition;
import net.tkg.facets.effect.TraitEffect;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A trait as defined in data/&lt;namespace&gt;/facets/trait/&lt;name&gt;.json
 *
 * @param name          display name, a text component (plain string or {"translate": ...})
 * @param description   shown in the tooltip on the selection screen
 * @param icon          item shown next to the name
 * @param cost          points spent when taken (negative values give points back), only used in POINTS mode
 * @param category      category the trait is listed under on the screen
 * @param selectable    false hides the trait from the screen; it can then only be granted by commands, occupations or other mods
 * @param exclusiveWith traits or trait tags that can't be held together with this one (checked both ways)
 * @param requires      traits or trait tags the player needs before taking this one
 * @param occupations   if not empty, only these occupations can pick the trait
 * @param showEffects   false hides the generated effect lines from the tooltip
 * @param effects       what the trait does
 * @param gainWhen      the player gains the trait during play once this condition matches
 * @param loseWhen      the player loses the trait during play once this condition matches
 * @param replaces      traits or trait tags removed when this trait is gained through gain_when
 * @param removableFromOccupation when given for free by an occupation, lose_when, replaces and the remove command
 *                                can still take the trait away
 * @param onGain        commands run once when the player gains the trait, as the server with @s as the player
 */
public record Trait(Component name, Component description, Item icon, int cost, Optional<ResourceLocation> category,
                    boolean selectable, List<TraitRef> exclusiveWith, List<TraitRef> requires,
                    List<ResourceLocation> occupations, boolean showEffects, List<TraitEffect> effects,
                    Optional<EffectCondition> gainWhen, Optional<EffectCondition> loseWhen, List<TraitRef> replaces,
                    boolean removableFromOccupation, List<String> onGain) {

    public static final Codec<Trait> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Trait::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", CommonComponents.EMPTY).forGetter(Trait::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PAPER).forGetter(Trait::icon),
            Codec.INT.optionalFieldOf("cost", 0).forGetter(Trait::cost),
            ResourceLocation.CODEC.optionalFieldOf("category").forGetter(Trait::category),
            Codec.BOOL.optionalFieldOf("selectable", true).forGetter(Trait::selectable),
            TraitRef.CODEC.listOf().optionalFieldOf("exclusive_with", List.of()).forGetter(Trait::exclusiveWith),
            TraitRef.CODEC.listOf().optionalFieldOf("requires", List.of()).forGetter(Trait::requires),
            ResourceLocation.CODEC.listOf().optionalFieldOf("occupations", List.of()).forGetter(Trait::occupations),
            Codec.BOOL.optionalFieldOf("show_effects", true).forGetter(Trait::showEffects),
            TraitEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Trait::effects),
            EffectCondition.CODEC.optionalFieldOf("gain_when").forGetter(Trait::gainWhen),
            EffectCondition.CODEC.optionalFieldOf("lose_when").forGetter(Trait::loseWhen),
            TraitRef.CODEC.listOf().optionalFieldOf("replaces", List.of()).forGetter(Trait::replaces),
            Codec.BOOL.optionalFieldOf("removable_from_occupation", false).forGetter(Trait::removableFromOccupation),
            Codec.STRING.listOf().optionalFieldOf("on_gain", List.of()).forGetter(Trait::onGain)
    ).apply(instance, Trait::new));

    /** Every condition used by this trait, including nested ones and those inside conditional effects. */
    public void visitConditions(java.util.function.Consumer<EffectCondition> visitor) {
        gainWhen.ifPresent(condition -> condition.visit(visitor));
        loseWhen.ifPresent(condition -> condition.visit(visitor));
        effects.forEach(effect -> effect.visitConditions(visitor));
    }

    public boolean hasTriggers() {
        return gainWhen.isPresent() || loseWhen.isPresent();
    }

    /** Tooltip lines describing the effects, empty when show_effects is off. */
    public List<Component> effectLines() {
        List<Component> lines = new ArrayList<>();
        if (showEffects) {
            effects.forEach(effect -> effect.addTooltip(lines::add));
        }
        return lines;
    }

    /** Whether the trait can be picked with the given occupation (null when there is none). */
    public boolean availableFor(TraitHolder self, @Nullable ResourceLocation occupationId, @Nullable Occupation occupation) {
        if (!occupations.isEmpty() && (occupationId == null || !occupations.contains(occupationId))) {
            return false;
        }
        return occupation == null || occupation.blocked().stream().noneMatch(ref -> ref.matches(self));
    }

    public static boolean conflicts(TraitHolder a, TraitHolder b) {
        return a.value().exclusiveWith().stream().anyMatch(ref -> ref.matches(b))
                || b.value().exclusiveWith().stream().anyMatch(ref -> ref.matches(a));
    }
}
