package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import net.minecraft.world.InteractionResult;

public final class InteractionResultCompat {
    private InteractionResultCompat() {
    }

    public static InteractionResult success() {
        return result("SUCCESS");
    }

    public static InteractionResult consume() {
        return result("CONSUME");
    }

    public static InteractionResult pass() {
        return result("PASS");
    }

    private static InteractionResult result(String fieldName) {
        try {
            Field field = InteractionResult.class.getField(fieldName);
            Object value = field.get(null);
            if (value instanceof InteractionResult interactionResult) {
                return interactionResult;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return null;
    }
}
