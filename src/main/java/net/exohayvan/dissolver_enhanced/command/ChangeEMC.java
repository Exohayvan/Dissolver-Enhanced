package net.exohayvan.dissolver_enhanced.command;

import static java.util.stream.Collectors.joining;

import java.util.HashMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.data.StateSaverAndLoader;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;

public class ChangeEMC {
    public static int changeEMC(CommandContext<CommandSourceStack> context, String command) {
        final int value = IntegerArgumentType.getInteger(context, "number");

        Player player = context.getSource().getPlayer();
        updateEMCValue(player, command, context, value);

        return 1;
    }

    public static int changeEMCPlayer(CommandContext<CommandSourceStack> context, String command, Player player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        final int value = IntegerArgumentType.getInteger(context, "number");
        
        updateEMCValue(player, command, context, value);

        return 1;
    }

    public static void updateEMCValue(Player player, String key, CommandContext<CommandSourceStack> context, int inputValue) {
        int currentValue = EMCHelper.getEMCValue(player);

        if (key == "give") {
            currentValue += inputValue;
            ModCommands.feedback(context, Component.translatable("command.feedback.update.give", inputValue).getString() + currentValue);
        } else if (key == "take") {
            currentValue -= inputValue;
            ModCommands.feedback(context, Component.translatable("command.feedback.update.take", inputValue).getString() + currentValue);
        } else if (key == "set") {
            currentValue = inputValue;
            ModCommands.feedback(context, Component.translatable("command.feedback.update.set", currentValue).getString());
        }
        
        EMCHelper.setEMCValue(player, currentValue);
    }

    public static int listUserEMC(CommandContext<CommandSourceStack> context, String command) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        Player player = context.getSource().getPlayer();
        int currentEMC = EMCHelper.getEMCValue(player);

        ModCommands.feedback(context, Component.translatable("command.feedback.list.user", currentEMC).getString());
        return 1;
    }

    public static int getEMC(CommandContext<CommandSourceStack> context, String command) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        MinecraftServer server = context.getSource().getServer();
        final String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        PlayerData data = StateSaverAndLoader.getFromUuid(server, player.getUUID());

        ModCommands.feedback(context, Component.translatable("command.feedback.get", data.EMC).getString());
        return 1;
    }

    public static int listEMC(CommandContext<CommandSourceStack> context, String command) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Component.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        MinecraftServer server = context.getSource().getServer();
        HashMap<String, PlayerData> dataList = StateSaverAndLoader.getFullList(server);

        String msg = Component.translatable("command.feedback.list", dataList.size()).getString() + "§r\n" +
        dataList.entrySet()
        .stream()
        .map(a -> "- " + a.getKey() + ": §6" + a.getValue().EMC + "§r")
        .collect(joining("\n"));

        ModCommands.feedback(context, msg);
        return 1;
    }
}
