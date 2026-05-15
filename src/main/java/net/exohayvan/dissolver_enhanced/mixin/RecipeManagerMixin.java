package net.exohayvan.dissolver_enhanced.mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.recipe.RecipeManager;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.MinecraftVersionCompat;
import net.exohayvan.dissolver_enhanced.helpers.RecipeGenerator;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    // CUSTOM RECIPE
    @Inject(method = "apply", at = @At("HEAD"), require = 0)
    public void interceptApply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo info) {
        if (RecipeGenerator.DISSOLVER_RECIPE != null) {
            map.put(Identifier.of(DissolverEnhanced.MOD_ID, "dissolver_block_recipe"), RecipeGenerator.DISSOLVER_RECIPE);
        }
    }

    @Inject(method = "apply", at = @At("HEAD"), require = 0)
    private void applyMixin(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo info) {
        queueRecipeLoad(map);
    }

    @Inject(method = "method_64680", at = @At("RETURN"), require = 0, remap = false)
    private void prepareMixin(ResourceManager resourceManager, Profiler profiler, CallbackInfoReturnable<Object> info) {
        Map<Identifier, JsonElement> map = loadRecipeJson(resourceManager);
        HashMap<String, List<String>> tagItems = loadItemTags(resourceManager);
        if (RecipeGenerator.DISSOLVER_RECIPE != null) {
            map.put(Identifier.of(DissolverEnhanced.MOD_ID, "dissolver_block_recipe"), RecipeGenerator.DISSOLVER_RECIPE);
        }

        EMCValues.tagsLoaded(getTagEMCValues(tagItems), tagItems);
        queueRecipeLoad(map);
    }

    private static void queueRecipeLoad(Map<Identifier, JsonElement> map) {
        EMCValues.beginStartup(map.size());
        RECIPES.clear();
        RECIPE_SOURCES.clear();
        RECIPE_JSON.clear();
        STONE_CUTTER_LIST.clear();

        // let tag items load before looking through recipes
        new Thread(() -> {
            wait(800);

            Iterator<Map.Entry<Identifier, JsonElement>> recipeIterator = map.entrySet().iterator();
            while (recipeIterator.hasNext()) {
                Map.Entry<Identifier, JsonElement> entry = recipeIterator.next();
                try {
                    if (!getJsonRecipe(entry)) {
                        EMCValues.incrementRecipesNotUnderstood();
                    }
                }catch (Exception e) {
                    EMCValues.incrementRecipesNotUnderstood();
                }
            }

            EMCValues.recipesLoaded(RECIPES, RECIPE_SOURCES, RECIPE_JSON, STONE_CUTTER_LIST);
        }).start();
    }

    private static Map<Identifier, JsonElement> loadRecipeJson(ResourceManager resourceManager) {
        Map<Identifier, JsonElement> recipes = new HashMap<>();
        loadRecipeJson(resourceManager, "recipe", recipes);
        loadRecipeJson(resourceManager, "recipes", recipes);
        loadNestedDatapackRecipeJson(resourceManager, recipes);
        return recipes;
    }

    private static void loadRecipeJson(ResourceManager resourceManager, String directory, Map<Identifier, JsonElement> recipes) {
        Map<Identifier, Resource> resources;
        try {
            resources = resourceManager.findResources(directory, id -> id.getPath().endsWith(".json"));
        } catch (RuntimeException exception) {
            return;
        }

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().getReader()) {
                recipes.put(stripRecipePath(directory, entry.getKey()), JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException exception) {
                EMCValues.incrementRecipesNotUnderstood();
            }
        }
    }

    private static Identifier stripRecipePath(String directory, Identifier resourceId) {
        String prefix = directory + "/";
        String path = resourceId.getPath();
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }

        return Identifier.of(resourceId.getNamespace(), path);
    }

    private static void loadNestedDatapackRecipeJson(ResourceManager resourceManager, Map<Identifier, JsonElement> recipes) {
        Map<Identifier, Resource> resources;
        try {
            resources = resourceManager.findResources("datapacks", id ->
                id.getPath().contains("/data/") && isNestedDataPath(id.getPath(), "recipe") && id.getPath().endsWith(".json")
            );
        } catch (RuntimeException exception) {
            return;
        }

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier recipeId = stripNestedDataPath(entry.getKey(), "recipe");
            if (recipeId == null) continue;

            try (BufferedReader reader = entry.getValue().getReader()) {
                recipes.put(recipeId, JsonParser.parseReader(reader));
            } catch (IOException | RuntimeException exception) {
                EMCValues.incrementRecipesNotUnderstood();
            }
        }
    }

    private static HashMap<String, List<String>> loadItemTags(ResourceManager resourceManager) {
        HashMap<String, List<String>> rawTags = new HashMap<>();
        loadItemTags(resourceManager, "tags/item", rawTags);
        loadItemTags(resourceManager, "tags/items", rawTags);
        loadNestedDatapackItemTags(resourceManager, rawTags);

        HashMap<String, List<String>> resolvedTags = new HashMap<>();
        for (String tagId : rawTags.keySet()) {
            resolvedTags.put(tagId, resolveTagItems(tagId, rawTags, new ArrayList<>()));
        }

        return resolvedTags;
    }

    private static void loadItemTags(ResourceManager resourceManager, String directory, HashMap<String, List<String>> tagItems) {
        Map<Identifier, Resource> resources;
        try {
            resources = resourceManager.findResources(directory, id -> id.getPath().endsWith(".json"));
        } catch (RuntimeException exception) {
            return;
        }

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            String tagId = stripTagPath(directory, entry.getKey()).toString();
            List<String> values = tagItems.getOrDefault(tagId, new ArrayList<>());

            try (BufferedReader reader = entry.getValue().getReader()) {
                loadTagValues(reader, values);
            } catch (IOException | RuntimeException exception) {
                EMCValues.incrementRecipesNotUnderstood();
            }

            if (!values.isEmpty()) {
                tagItems.put(tagId, values);
            }
        }
    }

    private static void loadNestedDatapackItemTags(ResourceManager resourceManager, HashMap<String, List<String>> tagItems) {
        Map<Identifier, Resource> resources;
        try {
            resources = resourceManager.findResources("datapacks", id ->
                id.getPath().contains("/data/") && (
                    isNestedDataPath(id.getPath(), "tags/item") || isNestedDataPath(id.getPath(), "tags/items")
                ) && id.getPath().endsWith(".json")
            );
        } catch (RuntimeException exception) {
            return;
        }

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier tagIdentifier = stripNestedDataPath(entry.getKey(), "tags/item");
            if (tagIdentifier == null) {
                tagIdentifier = stripNestedDataPath(entry.getKey(), "tags/items");
            }
            if (tagIdentifier == null) continue;

            String tagId = tagIdentifier.toString();
            List<String> values = tagItems.getOrDefault(tagId, new ArrayList<>());
            loadTagValues(entry.getValue(), values);

            if (!values.isEmpty()) {
                tagItems.put(tagId, values);
            }
        }
    }

    private static boolean isNestedDataPath(String path, String directory) {
        return path.contains("/" + directory + "/");
    }

    private static Identifier stripNestedDataPath(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        String marker = "/data/";
        int markerIndex = path.indexOf(marker);
        if (markerIndex < 0) return null;

        String dataPath = path.substring(markerIndex + marker.length());
        String directoryMarker = "/" + directory + "/";
        int directoryIndex = dataPath.indexOf(directoryMarker);
        if (directoryIndex <= 0) return null;

        String namespace = dataPath.substring(0, directoryIndex);
        String valuePath = dataPath.substring(directoryIndex + directoryMarker.length());
        if (valuePath.endsWith(".json")) {
            valuePath = valuePath.substring(0, valuePath.length() - ".json".length());
        }

        return Identifier.of(namespace, valuePath);
    }

    private static Identifier stripTagPath(String directory, Identifier resourceId) {
        String prefix = directory + "/";
        String path = resourceId.getPath();
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        if (path.endsWith(".json")) {
            path = path.substring(0, path.length() - ".json".length());
        }

        return Identifier.of(resourceId.getNamespace(), path);
    }

    private static void loadTagValues(Resource resource, List<String> values) {
        try (BufferedReader reader = resource.getReader()) {
            loadTagValues(reader, values);
        } catch (IOException | RuntimeException exception) {
            EMCValues.incrementRecipesNotUnderstood();
        }
    }

    private static void loadTagValues(BufferedReader reader, List<String> values) {
        JsonElement json = JsonParser.parseReader(reader);
        if (!json.isJsonObject()) return;

        JsonObject tagObject = json.getAsJsonObject();
        if (tagObject.has("replace") && tagObject.get("replace").getAsBoolean()) {
            values.clear();
        }

        if (!tagObject.has("values") || !tagObject.get("values").isJsonArray()) return;
        for (JsonElement value : tagObject.get("values").getAsJsonArray()) {
            String itemId = getTagValueId(value);
            if (!itemId.isEmpty() && !values.contains(itemId)) {
                values.add(itemId);
            }
        }
    }

    private static String getTagValueId(JsonElement value) {
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }

        if (!value.isJsonObject()) return "";

        JsonObject valueObject = value.getAsJsonObject();
        if (valueObject.has("id")) {
            return valueObject.get("id").getAsString();
        }

        return "";
    }

    private static List<String> resolveTagItems(
        String tagId,
        HashMap<String, List<String>> rawTags,
        List<String> resolving
    ) {
        List<String> resolved = new ArrayList<>();
        if (resolving.contains(tagId)) {
            return resolved;
        }

        resolving.add(tagId);
        for (String value : rawTags.getOrDefault(tagId, new ArrayList<>())) {
            if (value.startsWith("#")) {
                for (String nestedValue : resolveTagItems(value.substring(1), rawTags, resolving)) {
                    if (!resolved.contains(nestedValue)) {
                        resolved.add(nestedValue);
                    }
                }
            } else if (!resolved.contains(value)) {
                resolved.add(value);
            }
        }
        resolving.remove(tagId);

        return resolved;
    }

    private static HashMap<String, BigInteger> getTagEMCValues(HashMap<String, List<String>> tagItems) {
        HashMap<String, BigInteger> emcValues = new HashMap<>();
        for (Map.Entry<String, List<String>> tag : tagItems.entrySet()) {
            BigInteger emcValue = EMCValues.EMC_TAG_VALUES.get(tag.getKey());
            if (emcValue == null || emcValue.signum() == 0) continue;

            for (String itemId : tag.getValue()) {
                emcValues.put(itemId, emcValue);
            }
        }

        return emcValues;
    }

    @Inject(method = "method_17720", at = @At("HEAD"), require = 0, remap = false)
    private static void convertLegacyRecipeJsonForModernCodec(
        @Coerce Object recipeId,
        JsonObject recipeObject,
        @Coerce Object registries,
        CallbackInfoReturnable<Object> info
    ) {
        if (MinecraftVersionCompat.isLegacyRendererVersion()) {
            return;
        }

        convertLegacyIngredients(recipeObject);
    }

    private static void convertLegacyIngredients(JsonObject recipeObject) {
        if (recipeObject.has("key") && recipeObject.get("key").isJsonObject()) {
            JsonObject key = recipeObject.get("key").getAsJsonObject();
            for (String keyName : new ArrayList<>(key.keySet())) {
                JsonElement converted = convertLegacyIngredient(key.get(keyName));
                if (converted != null) {
                    key.add(keyName, converted);
                }
            }
        }

        if (recipeObject.has("ingredients") && recipeObject.get("ingredients").isJsonArray()) {
            for (int i = 0; i < recipeObject.get("ingredients").getAsJsonArray().size(); i++) {
                JsonElement ingredient = recipeObject.get("ingredients").getAsJsonArray().get(i);
                JsonElement converted = convertLegacyIngredient(ingredient);
                if (converted != null) {
                    recipeObject.get("ingredients").getAsJsonArray().set(i, converted);
                }
            }
        }

        for (String field : List.of("ingredient", "template", "base", "addition")) {
            if (!recipeObject.has(field)) continue;

            JsonElement converted = convertLegacyIngredient(recipeObject.get(field));
            if (converted != null) {
                recipeObject.add(field, converted);
            }
        }
    }

    private static JsonElement convertLegacyIngredient(JsonElement ingredient) {
        if (ingredient == null || ingredient.isJsonNull() || ingredient.isJsonPrimitive()) {
            return null;
        }

        if (ingredient.isJsonArray()) {
            for (int i = 0; i < ingredient.getAsJsonArray().size(); i++) {
                JsonElement converted = convertLegacyIngredient(ingredient.getAsJsonArray().get(i));
                if (converted != null) {
                    ingredient.getAsJsonArray().set(i, converted);
                }
            }
            return null;
        }

        if (!ingredient.isJsonObject()) {
            return null;
        }

        JsonObject ingredientObject = ingredient.getAsJsonObject();
        if (ingredientObject.size() != 1) {
            return null;
        }

        if (ingredientObject.has("item")) {
            return new com.google.gson.JsonPrimitive(ingredientObject.get("item").getAsString());
        }

        if (ingredientObject.has("id")) {
            return new com.google.gson.JsonPrimitive(ingredientObject.get("id").getAsString());
        }

        if (ingredientObject.has("tag")) {
            return new com.google.gson.JsonPrimitive("#" + ingredientObject.get("tag").getAsString());
        }

        return null;
    }

    private static final HashMap<String, List<String>> RECIPES = new HashMap<String, List<String>>();
    private static final HashMap<String, String> RECIPE_SOURCES = new HashMap<String, String>();
    private static final HashMap<String, String> RECIPE_JSON = new HashMap<String, String>();
    private static final List<String> STONE_CUTTER_LIST = new ArrayList<>();

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

        if (ingredientJson.isJsonPrimitive()) {
            addJsonIngredientId(itemIds, ingredientJson.getAsString());
            return itemIds;
        }

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

    private static void addJsonIngredientId(List<String> itemIds, String ingredientId) {
        if (ingredientId == null || ingredientId.isEmpty()) return;

        if (ingredientId.startsWith("#")) {
            addJsonTagIngredient(itemIds, ingredientId.substring(1));
        } else {
            itemIds.add(ingredientId);
        }
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
