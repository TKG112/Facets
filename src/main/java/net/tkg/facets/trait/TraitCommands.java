package net.tkg.facets.trait;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.tkg.facets.Facets;

import java.util.List;

/** Runs the on_select / on_gain commands of occupations and traits. */
final class TraitCommands {
    /** Same level as functions and command blocks */
    private static final int PERMISSION_LEVEL = 4;

    private TraitCommands() {
    }

    /**
     * Runs each command as the server, at the player's position, with @s being the player. Output is
     * hidden from chat; commands that fail to parse or run are logged.
     */
    static void run(ServerPlayer player, List<String> commands, String owner) {
        if (commands.isEmpty() || player instanceof FakePlayer) {
            return;
        }
        Commands manager = player.server.getCommands();
        for (String command : commands) {
            String line = command.startsWith("/") ? command.substring(1) : command;
            CommandSourceStack source = player.createCommandSourceStack()
                    .withPermission(PERMISSION_LEVEL)
                    .withSuppressedOutput()
                    .withCallback((success, result) -> {
                        if (!success) {
                            Facets.LOGGER.warn("Command from {} failed for {}: {}", owner, player.getGameProfile().getName(), line);
                        }
                    });
            ParseResults<CommandSourceStack> parsed = manager.getDispatcher().parse(line, source);
            CommandSyntaxException error = Commands.getParseException(parsed);
            if (error != null) {
                Facets.LOGGER.warn("Couldn't parse command from {}: {} ({})", owner, line, error.getMessage());
                continue;
            }
            manager.performCommand(parsed, line);
        }
    }
}
