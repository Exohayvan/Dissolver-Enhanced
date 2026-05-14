package net.exohayvan.dissolver_enhanced.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.common.values.EmcOverrideFileEditor;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;

import java.math.BigInteger;

public class ValueEMC {
    private static final String RELOAD_REQUIRED = " Reload required to correct these prices.";

    public static int set(CommandContext<ServerCommandSource> context, String command) {
        ItemStack stack = heldStack(context);
        if (stack.isEmpty()) {
            ModCommands.feedback(context, "Hold an item to set its EMC override." + RELOAD_REQUIRED);
            return 0;
        }

        String itemId = itemId(stack);
        BigInteger value;
        try {
            value = EmcNumber.parse(StringArgumentType.getString(context, "amount"));
        } catch (NumberFormatException e) {
            ModCommands.feedback(context, "Invalid EMC value." + RELOAD_REQUIRED);
            return 0;
        }

        if (value.signum() <= 0) {
            ModCommands.feedback(context, "EMC value must be greater than 0." + RELOAD_REQUIRED);
            return 0;
        }

        EmcOverrideFileEditor.setItemValue(ModConfig.overridesFile(), itemId, value);
        ModCommands.feedback(context, "Set EMC override for " + itemId + " to " + EmcNumber.format(value) + "." + RELOAD_REQUIRED);
        return 1;
    }

    public static int get(CommandContext<ServerCommandSource> context, String command) {
        ItemStack stack = heldStack(context);
        if (stack.isEmpty()) {
            ModCommands.feedback(context, "Hold an item to get its EMC value.");
            return 0;
        }

        String itemId = itemId(stack);
        BigInteger overrideValue = EmcOverrideFileEditor.getItemValue(ModConfig.overridesFile(), itemId);
        BigInteger currentValue = EMCValues.getBig(itemId);
        String source = EMCValues.getSource(itemId);

        if (overrideValue != null) {
            ModCommands.feedback(context, itemId + " override: " + EmcNumber.format(overrideValue) + ". Current loaded value: " + EmcNumber.format(currentValue) + " (" + source + ").");
        } else {
            ModCommands.feedback(context, itemId + " has no override. Current loaded value: " + EmcNumber.format(currentValue) + " (" + source + ").");
        }
        return 1;
    }

    public static int clear(CommandContext<ServerCommandSource> context, String command) {
        ItemStack stack = heldStack(context);
        if (stack.isEmpty()) {
            ModCommands.feedback(context, "Hold an item to clear its EMC override." + RELOAD_REQUIRED);
            return 0;
        }

        String itemId = itemId(stack);
        boolean removed = EmcOverrideFileEditor.clearItemValue(ModConfig.overridesFile(), itemId);
        if (removed) {
            ModCommands.feedback(context, "Cleared EMC override for " + itemId + "." + RELOAD_REQUIRED);
        } else {
            ModCommands.feedback(context, itemId + " did not have an EMC override." + RELOAD_REQUIRED);
        }
        return removed ? 1 : 0;
    }

    private static ItemStack heldStack(CommandContext<ServerCommandSource> context) {
        PlayerEntity player = context.getSource().getPlayer();
        return player == null ? ItemStack.EMPTY : player.getMainHandStack();
    }

    private static String itemId(ItemStack stack) {
        return stack.getItem().toString();
    }
}
