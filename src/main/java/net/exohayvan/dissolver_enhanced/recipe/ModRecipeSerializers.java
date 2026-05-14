package net.exohayvan.dissolver_enhanced.recipe;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DissolverEnhanced.MOD_ID);

    public static final RegistryObject<RecipeSerializer<DynamicDissolverRecipe>> DYNAMIC_DISSOLVER =
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
