package net.exohayvan.dissolver_enhanced.command;

import com.mojang.brigadier.context.CommandContext;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
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
        return changeItem(context, context.getSource().getPlayer(), true);
    }

    public static int addPlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        return changeItem(context, player, true);
    }

    public static int remove(CommandContext<CommandSourceStack> context, String command) {
        return changeItem(context, context.getSource().getPlayer(), false);
    }

    public static int removePlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        return changeItem(context, player, false);
    }

    // HELPERS

    private static int changeItem(CommandContext<CommandSourceStack> context, Player player, boolean add) {
        boolean changed = add
            ? EMCHelper.learnItem(player, getItemId(context))
            : EMCHelper.forgetItem(player, getItemId(context));
        String action = add ? "add" : "remove";
        Component feedback = changed
            ? Component.translatable("command.feedback.memory." + action, getItemName(context))
            : Component.translatable("command.feedback.memory." + action + ".fail");
        ModCommands.feedback(context, feedback.getString());
        return 1;
    }

    private static String getItemId(CommandContext<CommandSourceStack> context) {
        final Item item = ItemArgument.getItem(context, "item").getItem();
        return ItemHelper.getId(item);
    }

    private static String getItemName(CommandContext<CommandSourceStack> context) {
        final Item item = ItemArgument.getItem(context, "item").getItem();
        return item.getDescription().getString();
    }
}
