package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public final class RecipeCompat {
    private RecipeCompat() {
    }

    public static Entry unwrap(Object value) {
        if (value instanceof Recipe<?> recipe) {
            return new Entry(recipe.getId(), recipe);
        }

        ResourceLocation id = null;
        Recipe<?> recipe = null;
        for (Method method : value.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (method.getReturnType() == ResourceLocation.class) {
                id = (ResourceLocation) invoke(method, value);
            } else if (Recipe.class.isAssignableFrom(method.getReturnType())) {
                recipe = (Recipe<?>) invoke(method, value);
            }
        }
        if (id == null || recipe == null) {
            throw new IllegalStateException("Unsupported recipe entry " + value.getClass().getName());
        }
        return new Entry(id, recipe);
    }

    private static Object invoke(Method method, Object target) {
        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
            throw new IllegalStateException("Could not unwrap recipe entry.", exception);
        }
    }

    public record Entry(ResourceLocation id, Recipe<?> recipe) {
    }
}
