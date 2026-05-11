package net.exohayvan.dissolver_enhanced.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.RecipeGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Shadow @Final private HolderLookup.Provider registryLookup;

    // CUSTOM RECIPE
    @Inject(method = "apply", at = @At("HEAD"))
    public void interceptApply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo info) {
        if (RecipeGenerator.DISSOLVER_RECIPE != null) {
            map.put(Identifier.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "dissolver_block_recipe"), RecipeGenerator.DISSOLVER_RECIPE);
        }
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void applyMixin(Map<Identifier, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo info) {
        EMCValues.beginStartup(map.size());
        RECIPES.clear();
        RECIPE_SOURCES.clear();
        RECIPE_JSON.clear();
        STONE_CUTTER_LIST.clear();
        RegistryOps<JsonElement> registryOps = this.registryLookup.createSerializationContext(JsonOps.INSTANCE);

        // let tag items load before looking through recipes
        new Thread(() -> {
            wait(800);

            Iterator<Map.Entry<Identifier, JsonElement>> recipeIterator = map.entrySet().iterator();
            while (recipeIterator.hasNext()) {
                Map.Entry<Identifier, JsonElement> entry = recipeIterator.next();
                try {
                    getRecipe(entry, registryOps);
                }catch (Exception e) {
                    if (!getJsonRecipe(entry)) {
                        EMCValues.incrementRecipesNotUnderstood();
                    }
                }
            }

            EMCValues.recipesLoaded(RECIPES, RECIPE_SOURCES, RECIPE_JSON, STONE_CUTTER_LIST);
        }).start();
    }

    private static final HashMap<String, List<String>> RECIPES = new HashMap<String, List<String>>();
    private static final HashMap<String, String> RECIPE_SOURCES = new HashMap<String, String>();
    private static final HashMap<String, String> RECIPE_JSON = new HashMap<String, String>();
    private static final List<String> STONE_CUTTER_LIST = new ArrayList<>();
    private void getRecipe(Map.Entry<Identifier, JsonElement> entry, RegistryOps<JsonElement> registryOps) {
        if (!getJsonRecipe(entry)) {
            EMCValues.incrementRecipesNotUnderstood();
        }
    }

    private static void addRecipe(String id, int extraEMC, List<String> INGREDIENTS, String recipeId, JsonElement rawJson) {
        // multiple recipes for same output
        int index = 1;
        while (RECIPES.containsKey(id + "__" + extraEMC + "__" + index)) {
            index++;
        }

        String recipeKey = id + "__" + extraEMC + "__" + index;
        RECIPES.put(recipeKey, INGREDIENTS);
        RECIPE_SOURCES.put(recipeKey, recipeId);
        RECIPE_JSON.put(recipeKey, rawJson.toString());
    }

    private static List<String> getJsonIngredientItemIds(JsonElement recipeJson, int ingredientIndex) {
        List<String> itemIds = new ArrayList<>();
        if (!recipeJson.isJsonObject()) return itemIds;

        JsonObject recipeObject = recipeJson.getAsJsonObject();
        if (!recipeObject.has("ingredients") || !recipeObject.get("ingredients").isJsonArray()) return itemIds;
        if (ingredientIndex >= recipeObject.get("ingredients").getAsJsonArray().size()) return itemIds;

        JsonElement ingredientJson = recipeObject.get("ingredients").getAsJsonArray().get(ingredientIndex);
        if (!ingredientJson.isJsonObject()) return itemIds;

        JsonObject ingredientObject = ingredientJson.getAsJsonObject();
        if (ingredientObject.has("item")) {
            itemIds.add(ingredientObject.get("item").getAsString());
        } else if (ingredientObject.has("tag")) {
            addJsonTagIngredient(itemIds, ingredientObject.get("tag").getAsString());
        }

        return itemIds;
    }

    private static boolean getJsonRecipe(Map.Entry<Identifier, JsonElement> entry) {
        if (!entry.getValue().isJsonObject()) return false;

        JsonObject recipeObject = entry.getValue().getAsJsonObject();
        if (!recipeObject.has("result")) return false;

        String type = recipeObject.has("type") ? recipeObject.get("type").getAsString() : "";
        JsonResult result = getJsonResult(recipeObject.get("result"));
        if (result == null || result.itemId.contains("minecraft:air") || result.itemId.contains("firework")) {
            return false;
        }

        boolean isCooking = type.contains("smelting") || type.contains("blasting") || type.contains("smoking") ||
            type.contains("campfire_cooking");
        if (isCooking && result.itemId.contains("nugget")) return false;

        List<String> ingredients = new ArrayList<>();
        HashMap<String, List<String>> replaceIngredients = new HashMap<>();

        if (type.contains("smithing")) {
            if (recipeObject.has("template")) {
                addJsonIngredient(ingredients, replaceIngredients, recipeObject.get("template"));
            }
            if (recipeObject.has("base")) {
                addJsonIngredient(ingredients, replaceIngredients, recipeObject.get("base"));
            }
            if (recipeObject.has("addition")) {
                addJsonIngredient(ingredients, replaceIngredients, recipeObject.get("addition"));
            }
        } else if (recipeObject.has("ingredient")) {
            addJsonIngredient(ingredients, replaceIngredients, recipeObject.get("ingredient"));
        } else if (recipeObject.has("ingredients") && recipeObject.get("ingredients").isJsonArray()) {
            for (JsonElement ingredient : recipeObject.get("ingredients").getAsJsonArray()) {
                addJsonIngredient(ingredients, replaceIngredients, ingredient);
            }
        } else if (recipeObject.has("pattern") && recipeObject.get("pattern").isJsonArray() &&
            recipeObject.has("key") && recipeObject.get("key").isJsonObject()) {
            JsonObject key = recipeObject.get("key").getAsJsonObject();
            for (JsonElement patternLine : recipeObject.get("pattern").getAsJsonArray()) {
                String line = patternLine.getAsString();
                for (int i = 0; i < line.length(); i++) {
                    String keyName = String.valueOf(line.charAt(i));
                    if (keyName.equals(" ") || !key.has(keyName)) continue;

                    addJsonIngredient(ingredients, replaceIngredients, key.get(keyName));
                }
            }
        }

        if (ingredients.isEmpty()) return false;

        boolean isOre = listSearch(ingredients, "ore");
        boolean isStone = listSearch(ingredients, "stone");
        addRecipe(
            result.itemId + "__" + result.count,
            isCooking && !isOre && !isStone ? 10 : 0,
            ingredients,
            entry.getKey().toString(),
            entry.getValue()
        );
        addReplacementRecipes(result.itemId, result.count, replaceIngredients, ingredients, entry.getKey().toString(), entry.getValue());

        return true;
    }

    private static void addJsonIngredient(
        List<String> ingredients,
        HashMap<String, List<String>> replaceIngredients,
        JsonElement ingredientJson
    ) {
        List<String> itemIds = getJsonIngredientItemIds(ingredientJson);
        if (itemIds.isEmpty()) return;

        String rootItemId = itemIds.get(0);
        ingredients.add(rootItemId);

        for (int i = 1; i < itemIds.size(); i++) {
            List<String> replace = new ArrayList<>();
            if (replaceIngredients.containsKey(rootItemId)) replace = replaceIngredients.get(rootItemId);
            replace.add(itemIds.get(i));
            replaceIngredients.put(rootItemId, replace);
        }
    }

    private static List<String> getJsonIngredientItemIds(JsonElement ingredientJson) {
        List<String> itemIds = new ArrayList<>();

        if (ingredientJson.isJsonArray()) {
            for (JsonElement alternative : ingredientJson.getAsJsonArray()) {
                itemIds.addAll(getJsonIngredientItemIds(alternative));
            }
            return itemIds;
        }

        if (!ingredientJson.isJsonObject()) return itemIds;

        JsonObject ingredientObject = ingredientJson.getAsJsonObject();
        if (ingredientObject.has("item")) {
            itemIds.add(ingredientObject.get("item").getAsString());
        } else if (ingredientObject.has("id")) {
            itemIds.add(ingredientObject.get("id").getAsString());
        } else if (ingredientObject.has("tag")) {
            addJsonTagIngredient(itemIds, ingredientObject.get("tag").getAsString());
        }

        return itemIds;
    }

    private static void addJsonTagIngredient(List<String> itemIds, String tagId) {
        if (EMCValues.EMC_TAG_VALUES.containsKey(tagId)) {
            itemIds.add("#" + tagId);
            return;
        }

        List<String> tagItems = EMCValues.getTagItems(tagId);
        if (tagItems.isEmpty()) {
            itemIds.add("#" + tagId);
            return;
        }

        itemIds.addAll(tagItems);
    }

    private static void addReplacementRecipes(
        String resultId,
        int resultCount,
        HashMap<String, List<String>> replaceIngredients,
        List<String> ingredients,
        String recipeId,
        JsonElement rawJson
    ) {
        if (replaceIngredients.isEmpty()) return;

        for (Map.Entry<String, List<String>> replace : replaceIngredients.entrySet()) {
            String key = replace.getKey();
            List<String> replacedIngredients = replace.getValue();

            for (String replacedIngredient : replacedIngredients) {
                List<String> newIngredients = new ArrayList<>();

                for (String ingredient : ingredients) {
                    newIngredients.add(key.contains(ingredient) ? replacedIngredient : ingredient);
                }

                addRecipe(resultId + "__" + resultCount, 0, newIngredients, recipeId, rawJson);
            }
        }
    }

    private static JsonResult getJsonResult(JsonElement resultJson) {
        if (resultJson.isJsonPrimitive()) {
            return new JsonResult(resultJson.getAsString(), 1);
        }

        if (!resultJson.isJsonObject()) return null;

        JsonObject resultObject = resultJson.getAsJsonObject();
        String itemId = null;
        if (resultObject.has("id")) itemId = resultObject.get("id").getAsString();
        if (itemId == null && resultObject.has("item")) itemId = resultObject.get("item").getAsString();
        if (itemId == null) return null;

        int count = resultObject.has("count") ? resultObject.get("count").getAsInt() : 1;
        return new JsonResult(itemId, count);
    }

    private static class JsonResult {
        private final String itemId;
        private final int count;

        private JsonResult(String itemId, int count) {
            this.itemId = itemId;
            this.count = count;
        }
    }

    static private boolean listSearch(List<String> INGREDIENTS, String keyId) {
        for (String key : INGREDIENTS){
            if (key.contains(keyId)) return true;
        }

        return false;
    }

    private static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
