package net.exohayvan.dissolver_enhanced.recipe;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
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
            () -> new SimpleCraftingRecipeSerializer<>(DynamicDissolverRecipe::new)
        );

    private ModRecipeSerializers() {
    }

    public static void init(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
