package net.exohayvan.dissolver_enhanced.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class RecipeMapAugmenterTest {
    @Test
    void appendsGeneratedRecipeWithoutDroppingExistingRecipes() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        ResourceKey<Recipe<?>> existingKey = recipeKey("existing");
        ResourceKey<Recipe<?>> generatedKey = recipeKey("dissolver_block_recipe");
        RecipeHolder<?> existing = new RecipeHolder<>(existingKey, testRecipe());
        RecipeHolder<?> generated = new RecipeHolder<>(generatedKey, testRecipe());
        RecipeMap original = RecipeMap.create(java.util.List.of(existing));

        RecipeMap augmented = RecipeMapAugmenter.append(original, generated);

        assertThat(augmented.values()).containsExactlyInAnyOrder(existing, generated);
        assertThat(augmented.byKey(existingKey)).isSameAs(existing);
        assertThat(augmented.byKey(generatedKey)).isSameAs(generated);
    }

    @Test
    void replacesARecipeWithTheGeneratedRecipeWhenIdsMatch() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        ResourceKey<Recipe<?>> recipeKey = recipeKey("dissolver_block_recipe");
        RecipeHolder<?> existing = new RecipeHolder<>(recipeKey, testRecipe());
        RecipeHolder<?> generated = new RecipeHolder<>(recipeKey, testRecipe());
        RecipeMap original = RecipeMap.create(java.util.List.of(existing));

        RecipeMap augmented = RecipeMapAugmenter.append(original, generated);

        assertThat(augmented.values()).containsExactly(generated);
        assertThat(augmented.byKey(recipeKey)).isSameAs(generated);
    }

    private static ResourceKey<Recipe<?>> recipeKey(String path) {
        return ResourceKey.create(
            Registries.RECIPE,
            Identifier.fromNamespaceAndPath("dissolver_enhanced", path)
        );
    }

    private static Recipe<CraftingInput> testRecipe() {
        return new Recipe<>() {
            @Override
            public boolean matches(CraftingInput input, Level level) {
                return false;
            }

            @Override
            public ItemStack assemble(CraftingInput input) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean showNotification() {
                return false;
            }

            @Override
            public String group() {
                return "";
            }

            @Override
            public RecipeSerializer<? extends Recipe<CraftingInput>> getSerializer() {
                return null;
            }

            @Override
            public RecipeType<? extends Recipe<CraftingInput>> getType() {
                return RecipeType.CRAFTING;
            }

            @Override
            public PlacementInfo placementInfo() {
                return PlacementInfo.NOT_PLACEABLE;
            }

            @Override
            public RecipeBookCategory recipeBookCategory() {
                return new RecipeBookCategory();
            }
        };
    }
}
