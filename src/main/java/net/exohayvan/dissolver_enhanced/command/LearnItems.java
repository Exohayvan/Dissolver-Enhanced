package net.exohayvan.dissolver_enhanced.command;

import com.mojang.brigadier.context.CommandContext;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.function.BiFunction;

public class LearnItems {
    public static int everything(CommandContext<CommandSourceStack> context, String command) {
        Player player = context.getSource().getPlayer();

        EMCHelper.learnAllItems(player);
        
        ModCommands.feedback(context, Component.translatable("command.feedback.memory.fill").getString());
        return 1;
    }

    public static int everythingPlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        EMCHelper.learnAllItems(player);
        
        ModCommands.feedback(context, Component.translatable("command.feedback.memory.fill").getString());
        return 1;
    }

    public static int forget(CommandContext<CommandSourceStack> context, String command) {
        Player player = context.getSource().getPlayer();

        EMCHelper.forgetAllItems(player);
        
        ModCommands.feedback(context, Component.translatable("command.feedback.memory.forget").getString());
        return 1;
    }

    public static int forgetPlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        EMCHelper.forgetAllItems(player);
        
        ModCommands.feedback(context, Component.translatable("command.feedback.memory.forget").getString());
        return 1;
    }

    public static int add(CommandContext<CommandSourceStack> context, String command) {
        return modifyItem(
            context,
            context.getSource().getPlayer(),
            EMCHelper::learnItem,
            "command.feedback.memory.add",
            "command.feedback.memory.add.fail"
        );
    }

    public static int addPlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        return modifyItem(
            context,
            player,
            EMCHelper::learnItem,
            "command.feedback.memory.add",
            "command.feedback.memory.add.fail"
        );
    }

    public static int remove(CommandContext<CommandSourceStack> context, String command) {
        return modifyItem(
            context,
            context.getSource().getPlayer(),
            EMCHelper::forgetItem,
            "command.feedback.memory.remove",
            "command.feedback.memory.remove.fail"
        );
    }

    public static int removePlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        return modifyItem(
            context,
            player,
            EMCHelper::forgetItem,
            "command.feedback.memory.remove",
            "command.feedback.memory.remove.fail"
        );
    }

    // HELPERS

    private static int modifyItem(
        CommandContext<CommandSourceStack> context,
        Player player,
        BiFunction<Player, String, Boolean> operation,
        String successTranslation,
        String failureTranslation
    ) {
        boolean changed = operation.apply(player, getItemId(context));
        String feedback = changed
            ? Component.translatable(successTranslation, getItemName(context)).getString()
            : Component.translatable(failureTranslation).getString();
        ModCommands.feedback(context, feedback);
        return 1;
    }

    private static String getItemId(CommandContext<CommandSourceStack> context) {
        final Item item = ItemArgument.getItem(context, "item").item().value();
        return item.toString();
    }

    private static String getItemName(CommandContext<CommandSourceStack> context) {
        final Item item = ItemArgument.getItem(context, "item").item().value();
        return item.getName(item.getDefaultInstance()).getString();
    }
}
