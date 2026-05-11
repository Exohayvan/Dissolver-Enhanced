package net.exohayvan.dissolver_enhanced.command;

import com.mojang.brigadier.context.CommandContext;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

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
        Player player = context.getSource().getPlayer();
        boolean learned = EMCHelper.learnItem(player, getItemId(context));

        if (learned) ModCommands.feedback(context, Component.translatable("command.feedback.memory.add", getItemName(context)).getString());
        else ModCommands.feedback(context, Component.translatable("command.feedback.memory.add.fail").getString());

        return 1;
    }

    public static int addPlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        boolean learned = EMCHelper.learnItem(player, getItemId(context));

        if (learned) ModCommands.feedback(context, Component.translatable("command.feedback.memory.add", getItemName(context)).getString());
        else ModCommands.feedback(context, Component.translatable("command.feedback.memory.add.fail").getString());

        return 1;
    }

    public static int remove(CommandContext<CommandSourceStack> context, String command) {
        Player player = context.getSource().getPlayer();
        boolean removed = EMCHelper.forgetItem(player, getItemId(context));

        if (removed) ModCommands.feedback(context, Component.translatable("command.feedback.memory.remove", getItemName(context)).getString());
        else ModCommands.feedback(context, Component.translatable("command.feedback.memory.remove.fail").getString());

        return 1;
    }

    public static int removePlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        boolean removed = EMCHelper.forgetItem(player, getItemId(context));
        
        if (removed) ModCommands.feedback(context, Component.translatable("command.feedback.memory.remove", getItemName(context)).getString());
        else ModCommands.feedback(context, Component.translatable("command.feedback.memory.remove.fail").getString());

        return 1;
    }

    // HELPERS

    private static String getItemId(CommandContext<CommandSourceStack> context) {
        final Item item = ItemArgument.getItem(context, "item").getItem();
        return item.toString();
    }

    private static String getItemName(CommandContext<CommandSourceStack> context) {
        final Item item = ItemArgument.getItem(context, "item").getItem();
        return item.getDescription().getString();
    }
}
