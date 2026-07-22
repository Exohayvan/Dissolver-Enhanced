package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class BlockPropertiesCompat {
    private BlockPropertiesCompat() {
    }

    public static BlockBehaviour.Properties copy(Block block) {
        for (Method method : BlockBehaviour.Properties.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1 &&
                method.getParameterTypes()[0] == BlockBehaviour.class &&
                method.getReturnType() == BlockBehaviour.Properties.class) {
                try {
                    method.setAccessible(true);
                    return (BlockBehaviour.Properties) method.invoke(null, block);
                } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                    throw new IllegalStateException("Could not copy block properties.", exception);
                }
            }
        }
        throw new IllegalStateException("BlockBehaviour.Properties has no compatible block-copy factory.");
    }
}
