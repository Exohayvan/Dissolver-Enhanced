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
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.helpers.RecipeGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.JsonOps;
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
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    // CUSTOM RECIPE
    @Inject(method = "apply", at = @At("HEAD"))
    public void interceptApply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo info) {
        if (RecipeGenerator.DISSOLVER_RECIPE != null) {
            map.put(ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "dissolver_block_recipe"), RecipeGenerator.DISSOLVER_RECIPE);
        }
    }

    @Inject(method = "apply", at = @At("HEAD"))
    private void applyMixin(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo info) {
        EMCValues.beginStartup(map.size());
        RECIPES.clear();
        RECIPE_SOURCES.clear();
        RECIPE_JSON.clear();
        STONE_CUTTER_LIST.clear();
        // let tag items load before looking through recipes
        new Thread(() -> {
            wait(800);

            Iterator<Map.Entry<ResourceLocation, JsonElement>> recipeIterator = map.entrySet().iterator();
            while (recipeIterator.hasNext()) {
                Map.Entry<ResourceLocation, JsonElement> entry = recipeIterator.next();
                try {
                    getRecipe(entry);
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
    private void getRecipe(Map.Entry<ResourceLocation, JsonElement> entry) {
        ResourceLocation identifier = entry.getKey(); // JSON RECIPE FILE NAME
        String recipeId = identifier.toString();
        Recipe<?> recipe = Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), entry.getValue().getAsJsonObject()).getOrThrow(JsonParseException::new);

        // could filter by crafting only, but nice to have all recipes like smelting & stone cutting for more coverage!
        // if (recipe.getType() != RecipeType.CRAFTING) return;
        if (recipe.getType() == RecipeType.SMITHING) {
            if (!getJsonRecipe(entry)) {
                EMCValues.incrementRecipesNotUnderstood();
            }
            return;
        }
        
        ItemStack resultItem = recipe.getResultItem(registries);
        String resultId = ItemHelper.getId(resultItem.getItem());
        int resultCount = resultItem.getCount();

        boolean isCooking = recipe.getType() == RecipeType.SMELTING || recipe.getType() == RecipeType.BLASTING || recipe.getType() == RecipeType.SMOKING || recipe.getType() == RecipeType.CAMPFIRE_COOKING;

        // smelting armor/tools will give nuggets - that should not be the same emc value!!
        if ((isCooking) && resultId.contains("nugget")) return;

        List<String> INGREDIENTS = new ArrayList<>();
        HashMap<String, List<String>> REPLACE_INGREDIENTS = new HashMap<>();
        boolean hasUnresolvedIngredient = false;
        int ingredientIndex = -1;
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredientIndex++;
            // mostly just one ingredient (per slot) - but e.g. stone cutter can have multiple!
            int index = -1;
            for (int rawId : ingredient.getStackingIds()) {
                String itemId = ItemHelper.getId(Item.byId(rawId));
                index++;

                if (index == 0) {
                    INGREDIENTS.add(itemId);
                } else if (recipe.getType() == RecipeType.STONECUTTING) {
                    List<String> INGREDIENT = new ArrayList<>();
                    INGREDIENT.add(itemId);
                    addRecipe(resultId + "__" + 1, 0, INGREDIENT, recipeId, entry.getValue());
                } else if (resultId.contains("bed") || resultId.contains("glass")) {
                    // don't do anything
                    // bed: bed colors
                    // glass: smelting red sand
                } else {
                    // example: TNT is normally sand, but slot can also contain red_sand
                    // wool should only add white wool
                    String rootItemId = Item.byId(ingredient.getStackingIds().getInt(0)).toString();
                    if (!resultId.contains("wool") || itemId.contains("dye")) {
                        List<String> REPLACE = new ArrayList<>();
                        if (REPLACE_INGREDIENTS.containsKey(rootItemId)) REPLACE = REPLACE_INGREDIENTS.get(rootItemId);
                        REPLACE.add(itemId);
                        REPLACE_INGREDIENTS.put(rootItemId, REPLACE);
                    }
                }
            }

            if (index == -1) {
                List<String> jsonIngredientIds = getJsonIngredientItemIds(entry.getValue(), ingredientIndex);
                if (jsonIngredientIds.isEmpty()) hasUnresolvedIngredient = true;

                for (String itemId : jsonIngredientIds) {
                    index++;
                    if (index == 0) {
                        INGREDIENTS.add(itemId);
                    } else if (recipe.getType() == RecipeType.STONECUTTING) {
                        List<String> INGREDIENT = new ArrayList<>();
                        INGREDIENT.add(itemId);
                        addRecipe(resultId + "__" + 1, 0, INGREDIENT, recipeId, entry.getValue());
                    } else {
                        String rootItemId = getJsonIngredientItemIds(entry.getValue(), ingredientIndex).get(0);
                        List<String> REPLACE = new ArrayList<>();
                        if (REPLACE_INGREDIENTS.containsKey(rootItemId)) REPLACE = REPLACE_INGREDIENTS.get(rootItemId);
                        REPLACE.add(itemId);
                        REPLACE_INGREDIENTS.put(rootItemId, REPLACE);
                    }
                }
            }
        }

        if (hasUnresolvedIngredient) {
            if (getJsonRecipe(entry)) return;

            EMCValues.incrementRecipesNotUnderstood();
            return;
        }
        
        if (recipe.getType() == RecipeType.CRAFTING) {
            boolean DEFAULT_GLASS = INGREDIENTS.contains("minecraft:glass") || INGREDIENTS.contains("minecraft:glass_pane");
            if (resultId.startsWith("minecraft:") && resultId.contains("glass") && listSearch(INGREDIENTS, "glass") && !DEFAULT_GLASS) return; // glass into glass (dye)
            if (resultId.contains("carpet") && listSearch(INGREDIENTS, "carpet")) return; // carpet into carpet (dye)
            if (resultId.contains("bed") && listSearch(INGREDIENTS, "bed")) return; // bed into bed (dye)
            boolean NOT_WHITE_WOOL_OR_WHITE_DYE = !listSearch(INGREDIENTS, "white_wool") || !listSearch(INGREDIENTS, "dye");
            if (resultId.contains("wool") && listSearch(INGREDIENTS, "wool") && NOT_WHITE_WOOL_OR_WHITE_DYE) return; // wool into wool (dye) (only one recipe because of REPLACE_INGREDIENTS)
        }

        // some items does not have any ingredients, so manually add those! (it uses tags)
        if (INGREDIENTS.size() == 0) {
            // if (resultId.contains("_planks")) {
            //     String plank_type = resultId.substring(resultId.indexOf(":") + 1, resultId.indexOf("_planks"));
            //     if (resultId.contains("warped") || resultId.contains("crimson")) INGREDIENTS.add("minecraft:" + plank_type + "_stem");
            //     else if (resultId.contains("bamboo")) INGREDIENTS.add("minecraft:" + plank_type + "_block");
            //     else INGREDIENTS.add("minecraft:" + plank_type + "_log");
            // } else if (resultId == "minecraft:glass") {
            //     INGREDIENTS.add("minecraft:sand");
            // } else if (resultId == "minecraft:charcoal") {
            //     INGREDIENTS.add("minecraft:oak_log");
            // } else {
            //     DissolverEnhanced.LOGGER.info("FOUND ITEM RECIPE WITH NO INGREDIENTS: " + resultId);
            // }

            if (getJsonRecipe(entry)) return;

            if (!resultId.contains("minecraft:air") && !resultId.contains("firework") && !RECIPES.containsKey(resultId)) {
                EMCValues.incrementItemRecipesWithNoIngredients();
            }
            return;
        }

        // use this to debug items having weird multiple emc values!
        // if (resultId.contains("item_id")) DissolverEnhanced.LOGGER.info("Found with the type " + recipe.getType() + ". Ingredients: " + INGREDIENTS);

        boolean isOre = listSearch(INGREDIENTS, "ore");
        boolean isStone = listSearch(INGREDIENTS, "stone");

        // add extra EMC if cooking (because of fuel+time)
        addRecipe(resultId + "__" + resultCount, isCooking && !isOre && !isStone ? 10 : 0, INGREDIENTS, recipeId, entry.getValue());
        if (recipe.getType() == RecipeType.STONECUTTING && !STONE_CUTTER_LIST.contains(resultId)) STONE_CUTTER_LIST.add(resultId);

        if (REPLACE_INGREDIENTS.size() > 0) {
            for (Map.Entry<String, List<String>> replace : REPLACE_INGREDIENTS.entrySet()) {
                String key = replace.getKey();
                List<String> replacedIngredients = replace.getValue();

                for (String replacedIngredient : replacedIngredients) {
                    List<String> NEW_INGREDIENTS = new ArrayList<>();

                    for (String ingredient : INGREDIENTS) {
                        NEW_INGREDIENTS.add(key.contains(ingredient) ? replacedIngredient : ingredient);
                    }
                    
                    addRecipe(resultId + "__" + resultCount, 0, NEW_INGREDIENTS, recipeId, entry.getValue());
                }
            }
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

    private static boolean getJsonRecipe(Map.Entry<ResourceLocation, JsonElement> entry) {
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
