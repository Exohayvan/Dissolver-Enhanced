package net.exohayvan.dissolver_enhanced.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DissolverEnhanced.MOD_ID)
public class ForgeEmcValueLoader {
    private static final HashMap<String, List<String>> RECIPES = new HashMap<>();
    private static final HashMap<String, String> RECIPE_SOURCES = new HashMap<>();
    private static final HashMap<String, String> RECIPE_JSON = new HashMap<>();
    private static final List<String> STONE_CUTTER_LIST = new ArrayList<>();

    private ForgeEmcValueLoader() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        loadRecipes(event.getServer());
    }

    private static void loadRecipes(MinecraftServer server) {
        RECIPES.clear();
        RECIPE_SOURCES.clear();
        RECIPE_JSON.clear();
        STONE_CUTTER_LIST.clear();

        List<RecipeHolder<?>> recipes = new ArrayList<>(server.getRecipeManager().getRecipes());
        EMCValues.beginStartup(recipes.size());

        for (RecipeHolder<?> recipe : recipes) {
            try {
                addRecipe(server, recipe);
            } catch (RuntimeException exception) {
                EMCValues.incrementRecipesNotUnderstood();
                DissolverEnhanced.LOGGER.debug("Could not read recipe {} for EMC calculation.", recipe.id(), exception);
            }
        }

        EMCValues.recipesLoaded(RECIPES, RECIPE_SOURCES, RECIPE_JSON, STONE_CUTTER_LIST);
    }

    private static void addRecipe(MinecraftServer server, RecipeHolder<?> recipeHolder) {
        ResourceLocation recipeId = recipeHolder.id();
        Recipe<?> recipe = recipeHolder.value();
        RecipeType<?> recipeType = recipe.getType();

        ItemStack resultItem = recipe.getResultItem(server.registryAccess());
        String resultId = ItemHelper.getId(resultItem.getItem());
        int resultCount = resultItem.getCount();

        if (resultId.contains("minecraft:air") || resultId.contains("firework")) return;
        boolean isCooking = recipeType == RecipeType.SMELTING || recipeType == RecipeType.BLASTING ||
            recipeType == RecipeType.SMOKING || recipeType == RecipeType.CAMPFIRE_COOKING;
        if (isCooking && resultId.contains("nugget")) return;

        List<String> ingredients = new ArrayList<>();
        HashMap<String, List<String>> replaceIngredients = new HashMap<>();
        boolean hasUnresolvedIngredient = false;

        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
                continue;
            }

            int index = -1;
            String rootItemId = null;

            for (ItemStack stack : ingredient.getItems()) {
                if (stack.isEmpty()) {
                    continue;
                }

                String itemId = ItemHelper.getId(stack.getItem());
                index++;

                if (index == 0) {
                    rootItemId = itemId;
                    ingredients.add(itemId);
                } else if (recipeType == RecipeType.STONECUTTING) {
                    addRecipe(resultId + "__" + 1, 0, List.of(itemId), recipeId.toString());
                } else if (resultId.contains("bed") || resultId.contains("glass")) {
                    // Bed and glass color recipes should not create every dyed variant from one value path.
                } else if (!resultId.contains("wool") || itemId.contains("dye")) {
                    replaceIngredients.computeIfAbsent(rootItemId, ignored -> new ArrayList<>()).add(itemId);
                }
            }

            if (index == -1) {
                hasUnresolvedIngredient = true;
            }
        }

        if (hasUnresolvedIngredient) {
            EMCValues.incrementRecipesNotUnderstood();
            return;
        }

        if (ingredients.isEmpty()) {
            if (!resultId.contains("minecraft:air") && !resultId.contains("firework") && !RECIPES.containsKey(resultId)) {
                EMCValues.incrementItemRecipesWithNoIngredients();
            }
            return;
        }

        if (recipeType == RecipeType.CRAFTING && isBlockedDyeRecipe(resultId, ingredients)) return;

        boolean isOre = listSearch(ingredients, "ore");
        boolean isStone = listSearch(ingredients, "stone");
        addRecipe(resultId + "__" + resultCount, isCooking && !isOre && !isStone ? 10 : 0, ingredients, recipeId.toString());

        if (recipeType == RecipeType.STONECUTTING && !STONE_CUTTER_LIST.contains(resultId)) {
            STONE_CUTTER_LIST.add(resultId);
        }

        addReplacementRecipes(resultId, resultCount, replaceIngredients, ingredients, recipeId.toString());
    }

    private static boolean isBlockedDyeRecipe(String resultId, List<String> ingredients) {
        boolean defaultGlass = ingredients.contains("minecraft:glass") || ingredients.contains("minecraft:glass_pane");
        if (resultId.startsWith("minecraft:") && resultId.contains("glass") && listSearch(ingredients, "glass") && !defaultGlass) {
            return true;
        }
        if (resultId.contains("carpet") && listSearch(ingredients, "carpet")) return true;
        if (resultId.contains("bed") && listSearch(ingredients, "bed")) return true;

        boolean notWhiteWoolOrWhiteDye = !listSearch(ingredients, "white_wool") || !listSearch(ingredients, "dye");
        return resultId.contains("wool") && listSearch(ingredients, "wool") && notWhiteWoolOrWhiteDye;
    }

    private static void addReplacementRecipes(
        String resultId,
        int resultCount,
        HashMap<String, List<String>> replaceIngredients,
        List<String> ingredients,
        String recipeId
    ) {
        if (replaceIngredients.isEmpty()) return;

        for (Map.Entry<String, List<String>> replace : replaceIngredients.entrySet()) {
            String key = replace.getKey();
            for (String replacedIngredient : replace.getValue()) {
                List<String> newIngredients = new ArrayList<>();

                for (String ingredient : ingredients) {
                    newIngredients.add(key.contains(ingredient) ? replacedIngredient : ingredient);
                }

                addRecipe(resultId + "__" + resultCount, 0, newIngredients, recipeId);
            }
        }
    }

    private static void addRecipe(String id, int extraEMC, List<String> ingredients, String recipeId) {
        int index = 1;
        while (RECIPES.containsKey(id + "__" + extraEMC + "__" + index)) {
            index++;
        }

        String recipeKey = id + "__" + extraEMC + "__" + index;
        RECIPES.put(recipeKey, ingredients);
        RECIPE_SOURCES.put(recipeKey, recipeId);
        RECIPE_JSON.put(recipeKey, "Unavailable on Forge parsed recipe path.");
    }

    private static boolean listSearch(List<String> ingredients, String keyId) {
        for (String key : ingredients) {
            if (key.contains(keyId)) return true;
        }

        return false;
    }
}
