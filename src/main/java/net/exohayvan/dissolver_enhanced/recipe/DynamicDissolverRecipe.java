package net.exohayvan.dissolver_enhanced.recipe;

import net.exohayvan.dissolver_enhanced.block.ModBlocks;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class DynamicDissolverRecipe extends CustomRecipe {
    public DynamicDissolverRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        if (container.width() < 3 || container.height() < 3) return false;

        NonNullList<Ingredient> ingredients = getIngredients();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int ingredientIndex = row * 3 + column;
                int containerIndex = row * container.width() + column;
                if (!ingredients.get(ingredientIndex).test(container.getItem(containerIndex))) {
                    return false;
                }
            }
        }

        for (int slot = 0; slot < container.size(); slot++) {
            int row = slot / container.width();
            int column = slot % container.width();
            if ((row >= 3 || column >= 3) && !container.getItem(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput container, HolderLookup.Provider registries) {
        return getResultItem(registries).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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
