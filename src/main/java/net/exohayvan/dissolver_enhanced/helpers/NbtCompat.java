package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Method;
import java.util.Optional;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public final class NbtCompat {
    private NbtCompat() {
    }

    public static boolean hasKey(NbtCompound nbt, String key) {
        return nbt.get(key) != null;
    }

    public static String getString(NbtCompound nbt, String key) {
        return readString(nbt.get(key));
    }

    public static int getInt(NbtCompound nbt, String key) {
        return readInt(nbt.get(key));
    }

    private static String readString(NbtElement element) {
        if (element == null) return "";

        for (String methodName : new String[] { "comp_3831", "asString", "method_10714", "method_68658" }) {
            try {
                Method method = element.getClass().getMethod(methodName);
                Object value = method.invoke(element);
                if (value instanceof String stringValue) return stringValue;
                if (value instanceof Optional<?> optional && optional.orElse(null) instanceof String stringValue) return stringValue;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        String raw = element.toString();
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static int readInt(NbtElement element) {
        if (element == null) return 0;

        for (String methodName : new String[] { "intValue", "method_10701", "method_10698", "method_68659" }) {
            try {
                Method method = element.getClass().getMethod(methodName);
                Object value = method.invoke(element);
                if (value instanceof Number number) return number.intValue();
                if (value instanceof Optional<?> optional && optional.orElse(null) instanceof Number number) return number.intValue();
            } catch (ReflectiveOperationException ignored) {
            }
        }

        try {
            return Integer.parseInt(element.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
