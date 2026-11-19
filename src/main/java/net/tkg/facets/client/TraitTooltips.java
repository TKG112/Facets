package net.tkg.facets.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.tkg.facets.trait.SelectionContext;
import net.tkg.facets.trait.Trait;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class TraitTooltips {
    private static final int WIDTH = 200;

    private TraitTooltips() {
    }

    /** Context built from the data the server last sent. */
    static SelectionContext clientContext(List<ResourceLocation> granted) {
        return new SelectionContext(FacetsClient.data(), granted);
    }

    /**
     * Name, description, effect lines, requirements, then the optional cost and the reason the trait
     * can't be taken.
     */
    static List<FormattedCharSequence> build(Font font, TraitEntry entry, SelectionContext context,
                                             @Nullable Component cost, @Nullable Component blockReason) {
        Trait trait = entry.trait();
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(trait.name().copy().withStyle(ChatFormatting.WHITE).getVisualOrderText());

        Component description = trait.description();
        if (!description.getString().isEmpty()) {
            lines.addAll(font.split(description.copy().withStyle(ChatFormatting.GRAY), WIDTH));
        }

        List<Component> effects = trait.effectLines();
        if (!effects.isEmpty()) {
            lines.add(FormattedCharSequence.EMPTY);
            effects.forEach(effect -> lines.addAll(font.split(effect, WIDTH)));
        }

        if (!trait.requires().isEmpty()) {
            lines.add(FormattedCharSequence.EMPTY);
            Component names = ComponentUtils.formatList(trait.requires(), context::refName);
            lines.addAll(font.split(Component.translatable("facets.screen.requires_list", names).withStyle(ChatFormatting.GRAY), WIDTH));
        }

        if (trait.hasTriggers()) {
            lines.add(FormattedCharSequence.EMPTY);
            trait.gainWhen().ifPresent(condition -> lines.addAll(font.split(
                    Component.translatable("facets.screen.gain_when", condition.describe()).withStyle(ChatFormatting.GREEN), WIDTH)));
            trait.loseWhen().ifPresent(condition -> lines.addAll(font.split(
                    Component.translatable("facets.screen.lose_when", condition.describe()).withStyle(ChatFormatting.GOLD), WIDTH)));
        }

        if (cost != null) {
            lines.add(FormattedCharSequence.EMPTY);
            lines.add(Component.translatable("facets.screen.cost", cost).withStyle(ChatFormatting.GRAY).getVisualOrderText());
        }
        if (blockReason != null) {
            lines.addAll(font.split(blockReason.copy().withStyle(ChatFormatting.RED), WIDTH));
        }
        return lines;
    }
}
