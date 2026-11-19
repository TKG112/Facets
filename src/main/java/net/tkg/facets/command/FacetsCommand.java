package net.tkg.facets.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.neoforged.neoforge.resource.ResourcePackLoader;
import net.tkg.facets.Facets;
import net.tkg.facets.api.ChangeCause;
import net.tkg.facets.data.FacetsData;
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
 * /facets list|add|remove|reset|open|occupation &lt;player&gt; [trait|occupation]
 * /facets reload
 */
public final class FacetsCommand {
    private static final DynamicCommandExceptionType UNKNOWN_TRAIT =
            new DynamicCommandExceptionType(id -> Component.translatable("facets.command.unknown", String.valueOf(id)));
    private static final DynamicCommandExceptionType ALREADY_HAS =
            new DynamicCommandExceptionType(name -> Component.translatable("facets.command.already_has", name));
    private static final DynamicCommandExceptionType MISSING =
            new DynamicCommandExceptionType(name -> Component.translatable("facets.command.missing", name));
    private static final DynamicCommandExceptionType UNKNOWN_OCCUPATION =
            new DynamicCommandExceptionType(id -> Component.translatable("facets.command.unknown_occupation", String.valueOf(id)));

    private static final SuggestionProvider<CommandSourceStack> ALL_TRAITS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(TraitManager.data().traitIds(), builder);

    private static final SuggestionProvider<CommandSourceStack> OCCUPATIONS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(TraitManager.data().occupationIds(), builder);

    private static final SuggestionProvider<CommandSourceStack> PLAYER_TRAITS = (context, builder) ->
            SharedSuggestionProvider.suggestResource(
                    TraitManager.get(EntityArgument.getPlayer(context, "player")).traits(), builder);

    private FacetsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("facets")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(c -> reload(c.getSource())))
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
                                .executes(c -> open(c.getSource(), EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("occupation")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("clear")
                                        .executes(c -> setOccupation(c.getSource(), EntityArgument.getPlayer(c, "player"), null)))
                                .then(Commands.argument("occupation", ResourceLocationArgument.id())
                                        .suggests(OCCUPATIONS)
                                        .executes(c -> setOccupation(c.getSource(), EntityArgument.getPlayer(c, "player"),
                                                ResourceLocationArgument.getId(c, "occupation")))))));
    }

    /**
     * Same as /reload (new datapacks and new folders in the facets folder are picked up too), then
     * reports what Facets loaded.
     */
    private static int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        PackRepository repository = server.getPackRepository();
        Collection<String> selected = repository.getSelectedIds();
        Collection<String> ids = discoverNewPacks(server, repository, selected);

        source.sendSuccess(() -> Component.translatable("facets.command.reload.start"), true);
        server.reloadResources(ids).thenRun(() -> {
            TraitData data = FacetsData.get();
            int errors = FacetsData.lastErrors();
            source.sendSuccess(() -> Component.translatable("facets.command.reload.done",
                    data.traitCount(), data.categoryCount(), data.occupationCount(), data.tagCount()), true);
            if (errors > 0) {
                source.sendFailure(Component.translatable("facets.command.reload.errors", errors));
            }
        }).exceptionally(error -> {
            Facets.LOGGER.warn("Failed to reload", error);
            source.sendFailure(Component.translatable("commands.reload.failure"));
            return null;
        });
        return 1;
    }

    /** Selected packs plus any newly found pack the world hasn't disabled, like /reload does. */
    private static Collection<String> discoverNewPacks(MinecraftServer server, PackRepository repository, Collection<String> selected) {
        repository.reload();
        Collection<String> ids = new ArrayList<>(selected);
        Collection<String> disabled = server.getWorldData().getDataConfiguration().dataPacks().getDisabled();
        for (String id : repository.getAvailableIds()) {
            if (!disabled.contains(id) && !ids.contains(id)) {
                ids.add(id);
            }
        }
        ResourcePackLoader.reorderNewlyDiscoveredPacks(ids, selected, repository);
        return ids;
    }

    private static int setOccupation(CommandSourceStack source, ServerPlayer player, @Nullable ResourceLocation id) throws CommandSyntaxException {
        TraitData data = TraitManager.data();
        Occupation occupation = data.occupation(id);
        if (id != null && occupation == null) {
            throw UNKNOWN_OCCUPATION.create(id);
        }
        TraitManager.setState(player, TraitManager.get(player).traits(), Optional.ofNullable(id), ChangeCause.COMMAND);
        Component name = occupation == null ? Component.translatable("facets.command.occupation.none") : occupation.name();
        source.sendSuccess(() -> Component.translatable("facets.command.occupation", player.getDisplayName(), name), true);
        return 1;
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        TraitData data = TraitManager.data();
        TraitManager.get(player).occupation().ifPresent(id -> {
            Occupation occupation = data.occupation(id);
            Component name = occupation != null ? occupation.name() : Component.literal(id.toString());
            source.sendSuccess(() -> Component.translatable("facets.command.list.occupation", player.getDisplayName(), name), false);
        });
        List<ResourceLocation> traits = TraitManager.effectiveTraits(player);
        if (traits.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("facets.command.list.none", player.getDisplayName()), false);
            return 0;
        }
        Component names = ComponentUtils.formatList(traits, id -> name(data, id));
        source.sendSuccess(() -> Component.translatable("facets.command.list", player.getDisplayName(), names), false);
        return traits.size();
    }

    private static int add(CommandSourceStack source, ServerPlayer player, ResourceLocation id) throws CommandSyntaxException {
        TraitData data = TraitManager.data();
        if (data.trait(id) == null) {
            throw UNKNOWN_TRAIT.create(id);
        }
        List<ResourceLocation> traits = new ArrayList<>(TraitManager.get(player).traits());
        if (traits.contains(id)) {
            throw ALREADY_HAS.create(name(data, id));
        }
        traits.add(id);
        TraitManager.setTraits(player, traits, ChangeCause.COMMAND);
        source.sendSuccess(() -> Component.translatable("facets.command.added", name(data, id), player.getDisplayName()), true);
        return 1;
    }

    private static int remove(CommandSourceStack source, ServerPlayer player, ResourceLocation id) throws CommandSyntaxException {
        TraitData data = TraitManager.data();
        List<ResourceLocation> traits = new ArrayList<>(TraitManager.get(player).traits());
        if (!traits.remove(id)) {
            throw MISSING.create(name(data, id));
        }
        TraitManager.setTraits(player, traits, ChangeCause.COMMAND);
        source.sendSuccess(() -> Component.translatable("facets.command.removed", name(data, id), player.getDisplayName()), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        TraitManager.setState(player, List.of(), Optional.empty(), ChangeCause.RESET);
        TraitManager.OpenResult result = TraitManager.openSelection(player);
        source.sendSuccess(() -> Component.translatable("facets.command.reset", player.getDisplayName()), true);
        reportNotOpened(source, result);
        return 1;
    }

    private static int open(CommandSourceStack source, ServerPlayer player) {
        TraitManager.OpenResult result = TraitManager.openSelection(player);
        if (result != TraitManager.OpenResult.OPENED) {
            reportNotOpened(source, result);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("facets.command.open", player.getDisplayName()), true);
        return 1;
    }

    private static void reportNotOpened(CommandSourceStack source, TraitManager.OpenResult result) {
        switch (result) {
            case DISABLED -> source.sendFailure(Component.translatable("facets.command.open.disabled"));
            case NOTHING_TO_PICK -> source.sendFailure(Component.translatable("facets.command.open.empty")
                    .withStyle(ChatFormatting.RED));
            default -> {
            }
        }
    }

    private static Component name(TraitData data, ResourceLocation id) {
        Trait trait = data.trait(id);
        return trait != null ? trait.name() : Component.literal(id.toString());
    }
}
