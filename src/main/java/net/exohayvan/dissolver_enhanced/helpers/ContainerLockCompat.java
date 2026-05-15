package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Method;

import net.minecraft.inventory.ContainerLock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

public final class ContainerLockCompat {
    private static final String[] READ_METHODS = {"method_5473", "fromNbt"};
    private static final String[] WRITE_METHODS = {"method_5474", "writeNbt"};

    private ContainerLockCompat() {
    }

    public static ContainerLock read(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        ContainerLock lock = invokeRead(nbt, registryLookup, NbtCompound.class, RegistryWrapper.WrapperLookup.class);
        if (lock != null) {
            return lock;
        }

        lock = invokeRead(nbt, registryLookup, NbtCompound.class);
        if (lock != null) {
            return lock;
        }

        return ContainerLock.fromNbt(nbt);
    }

    public static void write(ContainerLock lock, NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (invokeWrite(lock, nbt, registryLookup, NbtCompound.class, RegistryWrapper.WrapperLookup.class)) {
            return;
        }

        if (invokeWrite(lock, nbt, registryLookup, NbtCompound.class)) {
            return;
        }

        if (lock.equals(ContainerLock.EMPTY)) {
            return;
        }

        String lockKey = lock.key();
        if (!lockKey.isEmpty()) {
            nbt.putString("Lock", lockKey);
        }
    }

    private static ContainerLock invokeRead(
        NbtCompound nbt,
        RegistryWrapper.WrapperLookup registryLookup,
        Class<?>... parameterTypes
    ) {
        for (String methodName : READ_METHODS) {
            try {
                Method method = ContainerLock.class.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                Object result = parameterTypes.length == 2
                    ? method.invoke(null, nbt, registryLookup)
                    : method.invoke(null, nbt);
                if (result instanceof ContainerLock lock) {
                    return lock;
                }
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Try the next runtime signature.
            }
        }

        return null;
    }

    private static boolean invokeWrite(
        ContainerLock lock,
        NbtCompound nbt,
        RegistryWrapper.WrapperLookup registryLookup,
        Class<?>... parameterTypes
    ) {
        for (String methodName : WRITE_METHODS) {
            try {
                Method method = ContainerLock.class.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                if (parameterTypes.length == 2) {
                    method.invoke(lock, nbt, registryLookup);
                } else {
                    method.invoke(lock, nbt);
                }
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                // Try the next runtime signature.
            }
        }

        return false;
    }
}
