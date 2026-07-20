package net.exohayvan.dissolver_enhanced.helpers;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

public final class RecipeMapAugmenter {
    private RecipeMapAugmenter() {
    }

    public static RecipeMap append(RecipeMap recipes, RecipeHolder<?> additionalRecipe) {
        List<RecipeHolder<?>> augmented = new ArrayList<>(recipes.values());
        augmented.removeIf(recipe -> recipe.id().equals(additionalRecipe.id()));
        augmented.add(additionalRecipe);
        return RecipeMap.create(augmented);
    }
}
