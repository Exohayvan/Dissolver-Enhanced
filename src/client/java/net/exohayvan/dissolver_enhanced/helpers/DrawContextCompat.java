package net.exohayvan.dissolver_enhanced.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Function;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class DrawContextCompat {
    private static final Function<Identifier, RenderLayer> GUI_LAYER = DrawContextCompat::getGuiTexturedLayer;

    private DrawContextCompat() {
    }

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        Object[] legacyArgs = new Object[] { texture, x, y, u, v, width, height, textureWidth, textureHeight };
        if (invoke(context, new Class<?>[] {
            Identifier.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class
        }, legacyArgs)) {
            return;
        }

        Object[] layeredArgs = new Object[] { GUI_LAYER, texture, x, y, u, v, width, height, textureWidth, textureHeight };
        if (invoke(context, new Class<?>[] {
            Function.class, Identifier.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class
        }, layeredArgs)) {
            return;
        }

        Object pipeline = getGuiTexturedPipeline();
        if (pipeline != null) {
            Object[] pipelineArgs = new Object[] { pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight };
            if (invokeNamed(context, new String[] { "method_25290", "drawTexture" }, new Class<?>[] {
                pipeline.getClass(), Identifier.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class
            }, pipelineArgs)) {
                return;
            }
        }

        throw new IllegalStateException("Could not find compatible DrawContext.drawTexture overload.");
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        Object[] legacyArgs = new Object[] { texture, x, y, width, height };
        if (invoke(context, new Class<?>[] {
            Identifier.class, int.class, int.class, int.class, int.class
        }, legacyArgs)) {
            return;
        }

        Object[] layeredArgs = new Object[] { GUI_LAYER, texture, x, y, width, height };
        if (invoke(context, new Class<?>[] {
            Function.class, Identifier.class, int.class, int.class, int.class, int.class
        }, layeredArgs)) {
            return;
        }

        Object pipeline = getGuiTexturedPipeline();
        if (pipeline != null) {
            Object[] pipelineArgs = new Object[] { pipeline, texture, x, y, width, height };
            if (invokeNamed(context, new String[] { "method_52706", "drawGuiTexture" }, new Class<?>[] {
                pipeline.getClass(), Identifier.class, int.class, int.class, int.class, int.class
            }, pipelineArgs)) {
                return;
            }
        }

        throw new IllegalStateException("Could not find compatible DrawContext.drawGuiTexture overload.");
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int textureWidth, int textureHeight, int u, int v, int x, int y, int width, int height) {
        Object[] legacyArgs = new Object[] { texture, textureWidth, textureHeight, u, v, x, y, width, height };
        if (invoke(context, new Class<?>[] {
            Identifier.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class
        }, legacyArgs)) {
            return;
        }

        Object[] layeredArgs = new Object[] { GUI_LAYER, texture, textureWidth, textureHeight, u, v, x, y, width, height };
        if (invoke(context, new Class<?>[] {
            Function.class, Identifier.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class
        }, layeredArgs)) {
            return;
        }

        Object pipeline = getGuiTexturedPipeline();
        if (pipeline != null) {
            Object[] pipelineArgs = new Object[] { pipeline, texture, x, y, width, height, u, v, textureWidth, textureHeight };
            if (invokeNamed(context, new String[] { "method_70846", "drawGuiTexture" }, new Class<?>[] {
                pipeline.getClass(), Identifier.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class
            }, pipelineArgs)) {
                return;
            }
        }

        throw new IllegalStateException("Could not find compatible DrawContext.drawGuiTexture overload.");
    }

    public static void drawText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
        if (invokeDrawText(context, new Class<?>[] {
            TextRenderer.class, Text.class, int.class, int.class, int.class, boolean.class
        }, new Object[] { textRenderer, text, x, y, opaqueColor(color), shadow })) {
            return;
        }

        throw new IllegalStateException("Could not find compatible DrawContext.drawText overload.");
    }

    public static void drawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, boolean shadow) {
        if (invokeDrawText(context, new Class<?>[] {
            TextRenderer.class, String.class, int.class, int.class, int.class, boolean.class
        }, new Object[] { textRenderer, text, x, y, opaqueColor(color), shadow })) {
            return;
        }

        throw new IllegalStateException("Could not find compatible DrawContext.drawText overload.");
    }

    private static boolean invokeDrawText(DrawContext context, Class<?>[] parameterTypes, Object[] arguments) {
        for (Method method : DrawContext.class.getDeclaredMethods()) {
            if ((!method.getName().equals("drawText") && !method.getName().equals("method_51439") &&
                    !method.getName().equals("method_51433")) || !hasParameters(method, parameterTypes)) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(context, arguments);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return false;
            }
        }

        return false;
    }

    private static boolean invokeNamed(DrawContext context, String[] methodNames, Class<?>[] parameterTypes, Object[] arguments) {
        for (String methodName : methodNames) {
            for (Method method : DrawContext.class.getDeclaredMethods()) {
                if (!method.getName().equals(methodName) || method.getReturnType() != Void.TYPE || !hasParameters(method, parameterTypes)) {
                    continue;
                }

                try {
                    method.setAccessible(true);
                    method.invoke(context, arguments);
                    return true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    return false;
                }
            }
        }

        return false;
    }

    private static boolean invoke(DrawContext context, Class<?>[] parameterTypes, Object[] arguments) {
        for (Method method : DrawContext.class.getDeclaredMethods()) {
            if (method.getReturnType() != Void.TYPE || !hasParameters(method, parameterTypes)) {
                continue;
            }

            try {
                method.setAccessible(true);
                method.invoke(context, arguments);
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return false;
            }
        }

        return false;
    }

    private static boolean hasParameters(Method method, Class<?>[] parameterTypes) {
        Class<?>[] actualTypes = method.getParameterTypes();
        if (actualTypes.length != parameterTypes.length) {
            return false;
        }

        for (int i = 0; i < actualTypes.length; i++) {
            if (!parameterTypes[i].isAssignableFrom(actualTypes[i]) && !actualTypes[i].isAssignableFrom(parameterTypes[i])) {
                return false;
            }
        }

        return true;
    }

    private static int opaqueColor(int color) {
        return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
    }

    private static RenderLayer getGuiTexturedLayer(Identifier texture) {
        RenderLayer layer = invokeRenderLayer("method_62277", texture);
        if (layer != null) {
            return layer;
        }

        layer = invokeRenderLayer("getGuiTextured", texture);
        return layer != null ? layer : RenderLayer.getGui();
    }

    private static Object getGuiTexturedPipeline() {
        Object pipeline = getStaticField("net.minecraft.class_10799", "field_56883");
        if (pipeline != null) {
            return pipeline;
        }

        return getStaticField("net.minecraft.client.gl.RenderPipelines", "GUI_TEXTURED");
    }

    private static Object getStaticField(String className, String fieldName) {
        try {
            Class<?> owner = Class.forName(className);
            Field field = owner.getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static RenderLayer invokeRenderLayer(String methodName, Identifier texture) {
        for (Method method : RenderLayer.class.getDeclaredMethods()) {
            if (!method.getName().equals(methodName) || !Modifier.isStatic(method.getModifiers()) ||
                    method.getReturnType() != RenderLayer.class || method.getParameterCount() != 1 ||
                    !method.getParameterTypes()[0].isAssignableFrom(Identifier.class)) {
                continue;
            }

            try {
                method.setAccessible(true);
                return (RenderLayer)method.invoke(null, texture);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return null;
            }
        }

        return null;
    }
}
