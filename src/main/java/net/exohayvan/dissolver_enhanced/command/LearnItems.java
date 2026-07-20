package net.exohayvan.dissolver_enhanced.command;

import com.mojang.brigadier.context.CommandContext;

import java.util.function.BiFunction;

import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;

public class LearnItems {
    public static int everything(CommandContext<ServerCommandSource> context, String command) {
        PlayerEntity player = context.getSource().getPlayer();

        EMCHelper.learnAllItems(player);
        
        ModCommands.feedback(context, Text.translatable("command.feedback.memory.fill").getString());
        return 1;
    }

    public static int everythingPlayer(CommandContext<ServerCommandSource> context, String command, PlayerEntity player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        EMCHelper.learnAllItems(player);
        
        ModCommands.feedback(context, Text.translatable("command.feedback.memory.fill").getString());
        return 1;
    }

    public static int forget(CommandContext<ServerCommandSource> context, String command) {
        PlayerEntity player = context.getSource().getPlayer();

        EMCHelper.forgetAllItems(player);
        
        ModCommands.feedback(context, Text.translatable("command.feedback.memory.forget").getString());
        return 1;
    }

    public static int forgetPlayer(CommandContext<ServerCommandSource> context, String command, PlayerEntity player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        EMCHelper.forgetAllItems(player);
        
        ModCommands.feedback(context, Text.translatable("command.feedback.memory.forget").getString());
        return 1;
    }

    public static int add(CommandContext<ServerCommandSource> context, String command) {
        return updateItem(
            context,
            context.getSource().getPlayer(),
            EMCHelper::learnItem,
            "command.feedback.memory.add",
            "command.feedback.memory.add.fail"
        );
    }

    public static int addPlayer(CommandContext<ServerCommandSource> context, String command, PlayerEntity player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        return updateItem(
            context,
            player,
            EMCHelper::learnItem,
            "command.feedback.memory.add",
            "command.feedback.memory.add.fail"
        );
    }

    public static int remove(CommandContext<ServerCommandSource> context, String command) {
        return updateItem(
            context,
            context.getSource().getPlayer(),
            EMCHelper::forgetItem,
            "command.feedback.memory.remove",
            "command.feedback.memory.remove.fail"
        );
    }

    public static int removePlayer(CommandContext<ServerCommandSource> context, String command, PlayerEntity player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        return updateItem(
            context,
            player,
            EMCHelper::forgetItem,
            "command.feedback.memory.remove",
            "command.feedback.memory.remove.fail"
        );
    }

    private static int updateItem(
        CommandContext<ServerCommandSource> context,
        PlayerEntity player,
        BiFunction<PlayerEntity, String, Boolean> update,
        String successTranslationKey,
        String failureTranslationKey
    ) {
        boolean updated = update.apply(player, getItemId(context));
        Text feedback = updated
            ? Text.translatable(successTranslationKey, getItemName(context))
            : Text.translatable(failureTranslationKey);
        ModCommands.feedback(context, feedback.getString());
        return 1;
    }

    // HELPERS

    private static String getItemId(CommandContext<ServerCommandSource> context) {
        final Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        return item.toString();
    }

    private static String getItemName(CommandContext<ServerCommandSource> context) {
        final Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
        return ItemHelper.getName(item);
    }
}
