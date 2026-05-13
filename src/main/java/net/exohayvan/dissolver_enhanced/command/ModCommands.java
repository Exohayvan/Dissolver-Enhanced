package net.exohayvan.dissolver_enhanced.command;

// import static com.mojang.brigadier.arguments.StringArgumentType.getString;
// import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.commands.Commands.argument;
// import static net.minecraft.server.command.CommandManager.*;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.helpers.WirelessDissolver;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DissolverEnhanced.MOD_ID)
public class ModCommands {
    private static CommandDispatcher<CommandSourceStack> activeDispatcher;
    private static CommandBuildContext activeRegistryAccess;

    private static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> command) {
        activeDispatcher.register(command);
    }

    private static void createCustomCommand(String command, CommandMethodInterface func) {
        registerCommand(
            createRootCommand(command)
            .executes(context -> {
                DissolverEnhanced.LOGGER.info("Executed command: " + command);
                return func.execute(context, command);
            })
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createRootCommand(String command) {
        return literal(command).requires(source -> source.hasPermission(2)); // requires OP
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createCommandWithPlayerArg(String command, ArgumentType<?> argType, CommandMethodInterface func, CommandMethodPlayerInterface playerFunc) {
        return literal(command).executes((context) -> executeCommand(context, command, func)).then(argument("player", StringArgumentType.string()).executes((context) -> executePlayerCommand(context, command, playerFunc)));
    }

    // private static LiteralArgumentBuilder<ServerCommandSource> createSubCommand(String command, String argId, ArgumentType<?> argType, CommandMethodInterface func) {
    //     return literal(command).then(argument(argId, argType).executes((context) -> executeCommand(context, command, func)));
    // }

    private static LiteralArgumentBuilder<CommandSourceStack> createPlayerSubCommand(String command, CommandMethodInterface func) {
        return literal(command).then(argument("player", StringArgumentType.string()).suggests(new PlayerSuggestionProvider()).executes((context) -> executeCommand(context, command, func)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSubCommandWithPlayerArg(String command, String argId, ArgumentType<?> argType, CommandMethodInterface func, CommandMethodPlayerInterface playerFunc) {
        return literal(command).then(argument(argId, argType).executes((context) -> executeCommand(context, command, func)).then(argument("player", StringArgumentType.string()).suggests(new PlayerSuggestionProvider()).executes((context) -> executePlayerCommand(context, command, playerFunc))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createItemSubCommandWithPlayerArg(String command, CommandBuildContext registryAccess, CommandMethodInterface func, CommandMethodPlayerInterface playerFunc) {
        return literal(command).then(argument("item", ItemArgument.item(registryAccess)).suggests(new ItemSuggestionProvider()).executes((context) -> executeCommand(context, command, func)).then(argument("player", StringArgumentType.string()).suggests(new PlayerSuggestionProvider()).executes((context) -> executePlayerCommand(context, command, playerFunc))));
    }
    
    // special example: msg = "\"%s × %s = %s\".formatted(value, value, result)"
    public static void feedback(CommandContext<CommandSourceStack> context, String msg) {
        context.getSource().sendSuccess(() -> Component.literal(msg), false);
    }
    
    // HELPERS
    
    public static Player playerFromName(CommandContext<CommandSourceStack> context, String playerName) {
        return context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
    }

    // EXECUTE

    private interface CommandMethodInterface {
        int execute(CommandContext<CommandSourceStack> context, String command);
    }

    private static int executeCommand(CommandContext<CommandSourceStack> context, String command, CommandMethodInterface func) {
        DissolverEnhanced.LOGGER.info("Executed command: " + command);
        return func.execute(context, command);
    }

    private interface CommandMethodPlayerInterface {
        int execute(CommandContext<CommandSourceStack> context, String command, Player player);
    }

    private static int executePlayerCommand(CommandContext<CommandSourceStack> context, String command, CommandMethodPlayerInterface func) {
        final String playerName = StringArgumentType.getString(context, "player");
        Player player = playerFromName(context, playerName);

        // Player currently has to be logged on to the server,
        // but it is possible to find & update the player state based on the stored name

        if (player == null) {
            feedback(context, Component.translatable("command.feedback.player.not_found", playerName).getString());
            return -1;
        }

        DissolverEnhanced.LOGGER.info("Executed command to player '" + playerName + "': " + command);
        return func.execute(context, command, player);
    }

    // private static int executeSubCommand(CommandContext<ServerCommandSource> context, String command, CommandMethodInterface func) {
    //     DissolverEnhanced.LOGGER.info("Executed subcommand: " + command);
    //     return func.execute(context, command);
    // }

    // INITIALIZE

    public static void init() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        activeDispatcher = event.getDispatcher();
        activeRegistryAccess = event.getBuildContext();
        // createCustomCommand("emctest", ModCommands::changeEMC);

        registerCommand(
            createRootCommand("emc")
            .executes((context) -> executeCommand(context, "emc", ChangeEMC::listUserEMC))
            .then(literal("list").executes((context) -> executeCommand(context, "list", ChangeEMC::listEMC)))
            .then(createPlayerSubCommand("get", ChangeEMC::getEMC))
            .then(literal("debug")
                .executes((context) -> executeCommand(context, "debug", DebugItem::summary))
                .then(literal("item").executes((context) -> executeCommand(context, "debug item", DebugItem::item)))
                .then(literal("recipe").executes((context) -> executeCommand(context, "debug recipe", DebugItem::recipe)))
                .then(literal("namespace")
                    .executes((context) -> executeCommand(context, "debug namespace", DebugItem::namespace))
                    .then(argument("namespace", StringArgumentType.word())
                        .executes((context) -> executeCommand(context, "debug namespace", DebugItem::namespaceFiltered)))))
            .then(createSubCommandWithPlayerArg("give", "number", StringArgumentType.word(), ChangeEMC::changeEMC, ChangeEMC::changeEMCPlayer))
            .then(createSubCommandWithPlayerArg("take", "number", StringArgumentType.word(), ChangeEMC::changeEMC, ChangeEMC::changeEMCPlayer))
            .then(createSubCommandWithPlayerArg("set", "number", StringArgumentType.word(), ChangeEMC::changeEMC, ChangeEMC::changeEMCPlayer))
        );

        // LEARNED (unlock all, learn specific items, unlearn)
        activeDispatcher.register(
            createRootCommand("emcmemory")
            .then(createCommandWithPlayerArg("fill", ItemArgument.item(activeRegistryAccess), LearnItems::everything, LearnItems::everythingPlayer))
            .then(createCommandWithPlayerArg("clear", ItemArgument.item(activeRegistryAccess), LearnItems::forget, LearnItems::forgetPlayer))
            .then(createItemSubCommandWithPlayerArg("add", activeRegistryAccess, LearnItems::add, LearnItems::addPlayer))
            .then(createItemSubCommandWithPlayerArg("remove", activeRegistryAccess, LearnItems::remove, LearnItems::removePlayer))
        );

        createCustomCommand("opendissolver", (context, command) -> {
            Player player = context.getSource().getPlayer();
            Level world = context.getSource().getLevel();

            if (!WirelessDissolver.open(player, world)) {
                ModCommands.feedback(context, Component.translatable("wireless_open.fail", WirelessDissolver.radius).getString());
            }

            return 1;
        });

        activeDispatcher = null;
        activeRegistryAccess = null;
    }
}
