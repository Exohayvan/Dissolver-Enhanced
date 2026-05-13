package net.exohayvan.dissolver_enhanced.command;

import static java.util.stream.Collectors.joining;

import java.math.BigInteger;
import java.util.HashMap;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.PlayerData;
import net.exohayvan.dissolver_enhanced.data.StateSaverAndLoader;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;

public class ChangeEMC {
    public static int changeEMC(CommandContext<ServerCommandSource> context, String command) {
        final BigInteger value = getEmcArgument(context);

        PlayerEntity player = context.getSource().getPlayer();
        updateEMCValue(player, command, context, value);

        return 1;
    }

    public static int changeEMCPlayer(CommandContext<ServerCommandSource> context, String command, PlayerEntity player) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        final BigInteger value = getEmcArgument(context);
        
        updateEMCValue(player, command, context, value);

        return 1;
    }

    public static void updateEMCValue(PlayerEntity player, String key, CommandContext<ServerCommandSource> context, BigInteger inputValue) {
        BigInteger currentValue = EMCHelper.getEMCValue(player);

        if (key == "give") {
            currentValue = currentValue.add(inputValue);
            ModCommands.feedback(context, Text.translatable("command.feedback.update.give", EmcNumber.format(inputValue)).getString() + EmcNumber.format(currentValue));
        } else if (key == "take") {
            currentValue = currentValue.subtract(inputValue);
            if (currentValue.signum() < 0) currentValue = BigInteger.ZERO;
            ModCommands.feedback(context, Text.translatable("command.feedback.update.take", EmcNumber.format(inputValue)).getString() + EmcNumber.format(currentValue));
        } else if (key == "set") {
            currentValue = inputValue;
            ModCommands.feedback(context, Text.translatable("command.feedback.update.set", EmcNumber.format(currentValue)).getString());
        }
        
        EMCHelper.setEMCValue(player, currentValue);
    }

    public static int listUserEMC(CommandContext<ServerCommandSource> context, String command) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        PlayerEntity player = context.getSource().getPlayer();
        BigInteger currentEMC = EMCHelper.getEMCValue(player);

        ModCommands.feedback(context, Text.translatable("command.feedback.list.user", EmcNumber.format(currentEMC)).getString());
        return 1;
    }

    public static int getEMC(CommandContext<ServerCommandSource> context, String command) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        MinecraftServer server = context.getSource().getServer();
        final String playerName = StringArgumentType.getString(context, "player");
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
        
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        PlayerData data = StateSaverAndLoader.getFromUuid(server, player.getUuid());

        ModCommands.feedback(context, Text.translatable("command.feedback.get", EmcNumber.format(data.EMC)).getString());
        return 1;
    }

    public static int listEMC(CommandContext<ServerCommandSource> context, String command) {
        if (!ModConfig.PRIVATE_EMC) {
            ModCommands.feedback(context, Text.translatable("command.feedback.shared_data").getString());
            return 1;
        }

        MinecraftServer server = context.getSource().getServer();
        HashMap<String, PlayerData> dataList = StateSaverAndLoader.getFullList(server);

        String msg = Text.translatable("command.feedback.list", dataList.size()).getString() + "§r\n" +
        dataList.entrySet()
        .stream()
        .map(a -> "- " + a.getKey() + ": §6" + EmcNumber.format(a.getValue().EMC) + "§r")
        .collect(joining("\n"));

        ModCommands.feedback(context, msg);
        return 1;
    }

    private static BigInteger getEmcArgument(CommandContext<ServerCommandSource> context) {
        return EmcNumber.parse(StringArgumentType.getString(context, "number"));
    }
}
