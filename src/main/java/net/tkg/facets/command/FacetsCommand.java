package net.tkg.facets.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.tkg.facets.trait.Trait;
import net.tkg.facets.trait.TraitManager;

import java.util.ArrayList;
import java.util.List;

/**
 * /facets list|add|remove|reset|open &lt;player&gt; [trait]
 */
public final class FacetsCommand {
    private static final DynamicCommandExceptionType UNKNOWN_TRAIT =
            new DynamicCommandExceptionType(id -> Component.translatable("facets.command.unknown", String.valueOf(id)));
    private static final DynamicCommandExceptionType ALREADY_HAS =
            new DynamicCommandExceptionType(name -> Component.translatable("facets.command.already_has", name));
    private static final DynamicCommandExceptionType MISSING =
            new DynamicCommandExceptionType(name -> Component.translatable("facets.command.missing", name));

    private static final SuggestionProvider<CommandSourceStack> ALL_TRAITS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(TraitManager.traits(context.getSource().getServer()).keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> PLAYER_TRAITS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    TraitManager.get(EntityArgument.getPlayer(context, "player")).traits(), builder);

    private FacetsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("facets")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> list(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("trait", ResourceLocationArgument.id())
                                        .suggests(ALL_TRAITS)
                                        .executes(c -> add(c.getSource(), EntityArgument.getPlayer(c, "player"),
                                                ResourceLocationArgument.getId(c, "trait"))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("trait", ResourceLocationArgument.id())
                                        .suggests(PLAYER_TRAITS)
                                        .executes(c -> remove(c.getSource(), EntityArgument.getPlayer(c, "player"),
                                                ResourceLocationArgument.getId(c, "trait"))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reset(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("open")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> open(c.getSource(), EntityArgument.getPlayer(c, "player"))))));
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        List<ResourceLocation> traits = TraitManager.get(player).traits();
        if (traits.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("facets.command.list.none", player.getDisplayName()), false);
            return 0;
        }
        Registry<Trait> registry = TraitManager.traits(source.getServer());
        Component names = ComponentUtils.formatList(traits, id -> name(registry, id));
        source.sendSuccess(() -> Component.translatable("facets.command.list", player.getDisplayName(), names), false);
        return traits.size();
    }

    private static int add(CommandSourceStack source, ServerPlayer player, ResourceLocation id) throws CommandSyntaxException {
        Registry<Trait> registry = TraitManager.traits(source.getServer());
        if (!registry.containsKey(id)) {
            throw UNKNOWN_TRAIT.create(id);
        }
        List<ResourceLocation> traits = new ArrayList<>(TraitManager.get(player).traits());
        if (traits.contains(id)) {
            throw ALREADY_HAS.create(name(registry, id));
        }
        traits.add(id);
        TraitManager.setTraits(player, traits);
        source.sendSuccess(() -> Component.translatable("facets.command.added", name(registry, id), player.getDisplayName()), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, ServerPlayer player, ResourceLocation id) throws CommandSyntaxException {
        Registry<Trait> registry = TraitManager.traits(source.getServer());
        List<ResourceLocation> traits = new ArrayList<>(TraitManager.get(player).traits());
        if (!traits.remove(id)) {
            throw MISSING.create(name(registry, id));
        }
        TraitManager.setTraits(player, traits);
        source.sendSuccess(() -> Component.translatable("facets.command.removed", name(registry, id), player.getDisplayName()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        TraitManager.setTraits(player, List.of());
        TraitManager.openSelection(player);
        source.sendSuccess(() -> Component.translatable("facets.command.reset", player.getDisplayName()), true);
        return 1;
    }

    private static int open(CommandSourceStack source, ServerPlayer player) {
        if (!TraitManager.openSelection(player)) {
            source.sendFailure(Component.translatable("facets.command.open.disabled"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("facets.command.open", player.getDisplayName()), true);
        return 1;
    }

    private static Component name(Registry<Trait> registry, ResourceLocation id) {
        Trait trait = registry.get(id);
        return trait != null ? trait.name() : Component.literal(id.toString());
    }
}
