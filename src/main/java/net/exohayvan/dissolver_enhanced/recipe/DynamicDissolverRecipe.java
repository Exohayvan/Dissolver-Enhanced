package net.exohayvan.dissolver_enhanced.recipe;

import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DynamicDissolverRecipe extends CustomRecipe {
    public DynamicDissolverRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (container.getWidth() < 3 || container.getHeight() < 3) return false;

        NonNullList<Ingredient> ingredients = getIngredients();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int ingredientIndex = row * 3 + column;
                int containerIndex = row * container.getWidth() + column;
                if (!ingredients.get(ingredientIndex).test(container.getItem(containerIndex))) {
                    return false;
                }
            }
        }

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            int row = slot / container.getWidth();
            int column = slot % container.getWidth();
            if ((row >= 3 || column >= 3) && !container.getItem(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ModBlocks.DISSOLVER_BLOCK_ITEM.get());
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        Ingredient frame = Ingredient.of(ModItems.CRYSTAL_FRAME_ITEM.get());
        Ingredient center = Ingredient.of(Items.NETHER_STAR);

        String difficulty = ModConfig.DIFFICULTY.toLowerCase();
        if (difficulty.contains("easy")) {
            frame = Ingredient.of(Items.GLASS_PANE);
            center = Ingredient.of(Items.REDSTONE);
        } else if (difficulty.contains("normal")) {
            center = Ingredient.of(Items.PHANTOM_MEMBRANE);
        }

        return NonNullList.of(
            Ingredient.EMPTY,
            frame, frame, frame,
            frame, center, frame,
            frame, frame, frame
        );
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.DYNAMIC_DISSOLVER.get();
    }

    @Override
    public boolean isSpecial() {
        return false;
    }
}
