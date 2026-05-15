package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Method;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;

public final class InventoryCompat {
    private InventoryCompat() {
    }

    public static void onOpen(Inventory inventory, PlayerEntity player) {
        invokeViewerHook(inventory, player, "onOpen", "method_5435");
    }

    public static void onClose(Inventory inventory, PlayerEntity player) {
        invokeViewerHook(inventory, player, "onClose", "method_5432");
    }

    private static void invokeViewerHook(Inventory inventory, PlayerEntity player, String namedName, String intermediaryName) {
        if (inventory == null || player == null) return;

        for (Method method : inventory.getClass().getMethods()) {
            if ((!method.getName().equals(namedName) && !method.getName().equals(intermediaryName)) ||
                    method.getParameterCount() != 1 ||
                    !method.getParameterTypes()[0].isAssignableFrom(player.getClass())) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(inventory, player);
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }
}
