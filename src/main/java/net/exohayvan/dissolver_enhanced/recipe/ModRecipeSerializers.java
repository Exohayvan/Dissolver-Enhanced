package net.exohayvan.dissolver_enhanced.recipe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, DissolverEnhanced.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicDissolverRecipe>> DYNAMIC_DISSOLVER =
        RECIPE_SERIALIZERS.register(
            "dynamic_dissolver",
            ModRecipeSerializers::createDynamicDissolverSerializer
        );

    private ModRecipeSerializers() {
    }

    public static void init(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }

    @SuppressWarnings("unchecked")
    private static RecipeSerializer<DynamicDissolverRecipe> createDynamicDissolverSerializer() {
        RecipeSerializer<DynamicDissolverRecipe> simple = createSerializer(
            "net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer",
            "net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer$Factory"
        );
        if (simple != null) {
            return simple;
        }

        RecipeSerializer<DynamicDissolverRecipe> custom = createSerializer(
            "net.minecraft.world.item.crafting.CustomRecipe$Serializer",
            "net.minecraft.world.item.crafting.CustomRecipe$Serializer$Factory"
        );
        if (custom != null) {
            return custom;
        }

        throw new IllegalStateException("Could not create dynamic dissolver recipe serializer.");
    }

    @SuppressWarnings("unchecked")
    private static RecipeSerializer<DynamicDissolverRecipe> createSerializer(String serializerClassName, String factoryClassName) {
        try {
            ClassLoader classLoader = ModRecipeSerializers.class.getClassLoader();
            Class<?> serializerClass = Class.forName(serializerClassName, false, classLoader);
            Class<?> factoryClass = Class.forName(factoryClassName, false, classLoader);
            Object factory = Proxy.newProxyInstance(
                classLoader,
                new Class<?>[] { factoryClass },
                (proxy, method, args) -> {
                    if ("create".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof CraftingBookCategory category) {
                        return new DynamicDissolverRecipe(category);
                    }
                    throw new UnsupportedOperationException("Unsupported recipe factory method: " + method);
                }
            );
            Constructor<?> constructor = serializerClass.getConstructor(factoryClass);
            return (RecipeSerializer<DynamicDissolverRecipe>)constructor.newInstance(factory);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
