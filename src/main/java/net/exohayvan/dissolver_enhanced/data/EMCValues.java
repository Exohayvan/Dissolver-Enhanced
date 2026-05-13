package net.exohayvan.dissolver_enhanced.data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.exohayvan.dissolver_enhanced.common.values.DefaultEmcValues;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.exohayvan.dissolver_enhanced.common.values.EmcValueSet;

import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class EMCValues {
    protected static final Set<String> CONFIG_OVERRIDDEN = new HashSet<>();

    protected static final HashMap<String, BigInteger> EMC_VALUES = new HashMap<String, BigInteger>();
    private static final HashMap<String, String> EMC_SOURCES = new HashMap<String, String>();
    private static final HashMap<String, String> EMC_SOURCE_DETAILS = new HashMap<String, String>();
    private static final HashMap<String, BigInteger> CLIENT_SYNC_VALUES = new HashMap<String, BigInteger>();
    public static final HashMap<String, BigInteger> EMC_TAG_VALUES = new HashMap<String, BigInteger>();
    private static final HashMap<String, List<String>> TAG_ITEMS = new HashMap<String, List<String>>();

    public static Integer get(String key) {
        return EmcNumber.toIntSaturated(getBig(key));
    }

    public static BigInteger getBig(String key) {
        BigInteger emc = EMC_VALUES.getOrDefault(key, BigInteger.ZERO);
        if (emc.signum() > 0 || !EMCKey.isComponentKey(key)) return emc;

        return EMC_VALUES.getOrDefault(EMCKey.baseItemId(key), BigInteger.ZERO);
    }

    public static Integer getDisplay(String key) {
        return EmcNumber.toIntSaturated(getDisplayBig(key));
    }

    public static BigInteger getDisplayBig(String key) {
        BigInteger emc = CLIENT_SYNC_VALUES.getOrDefault(key, BigInteger.ZERO);
        if (emc.signum() > 0 || !EMCKey.isComponentKey(key)) return CLIENT_SYNC_VALUES.getOrDefault(key, getBig(key));

        return CLIENT_SYNC_VALUES.getOrDefault(EMCKey.baseItemId(key), getBig(key));
    }

    public static String getSource(String key) {
        String source = EMC_SOURCES.getOrDefault(key, "None");
        if (!source.equals("None") || !EMCKey.isComponentKey(key)) return source;

        return EMC_SOURCES.getOrDefault(EMCKey.baseItemId(key), "None");
    }

    public static String getSourceDetail(String key) {
        String sourceDetail = EMC_SOURCE_DETAILS.getOrDefault(key, "None");
        if (!sourceDetail.equals("None") || !EMCKey.isComponentKey(key)) return sourceDetail;

        return EMC_SOURCE_DETAILS.getOrDefault(EMCKey.baseItemId(key), "None");
    }

    public static List<String> getTagItems(String tagId) {
        return TAG_ITEMS.getOrDefault(tagId, new ArrayList<>());
    }

    public record RecipeUnlockInfo(int count, String reason) {}

    public static HashMap<String, Integer> getRecipeUnlockCounts() {
        HashMap<String, RecipeUnlockInfo> unlockInfos = getRecipeUnlockInfos();
        HashMap<String, Integer> unlockCounts = new HashMap<>();
        unlockInfos.forEach((itemId, info) -> unlockCounts.put(itemId, info.count()));
        return unlockCounts;
    }

    public static HashMap<String, RecipeUnlockInfo> getRecipeUnlockInfos() {
        return getRecipeUnlockInfos(null);
    }

    public static HashMap<String, RecipeUnlockInfo> getRecipeUnlockInfos(String namespace) {
        HashMap<String, Set<String>> unlockableResults = new HashMap<>();

        for (Map.Entry<String, List<String>> recipe : RECIPES.entrySet()) {
            String resultId = recipe.getKey().split("__")[0];
            if (namespace != null && !resultId.startsWith(namespace + ":")) continue;
            if (getBig(resultId).signum() > 0) continue;

            Set<String> missingIngredients = new HashSet<>();
            for (String ingredient : recipe.getValue()) {
                if (getRecipeIngredientValue(ingredient).signum() == 0) {
                    missingIngredients.add(ingredient);
                }
            }

            if (missingIngredients.size() != 1) continue;

            String missingIngredient = missingIngredients.iterator().next();
            Set<String> resultIds = new HashSet<>();
            if (unlockableResults.containsKey(missingIngredient)) {
                resultIds = unlockableResults.get(missingIngredient);
            }
            resultIds.add(resultId);
            unlockableResults.put(missingIngredient, resultIds);
        }

        HashMap<String, RecipeUnlockInfo> unlockInfos = new HashMap<>();
        unlockableResults.forEach((itemId, resultIds) ->
            unlockInfos.put(itemId, new RecipeUnlockInfo(resultIds.size(), getMissingRecipeIngredientReason(itemId)))
        );
        return unlockInfos;
    }

    private static BigInteger getRecipeIngredientValue(String itemId) {
        BigInteger emc = getIngredientEMC(itemId);
        if (emc.signum() > 0) return emc;

        if (RECIPE_ITEM_VALUES.containsKey(itemId)) {
            return getAverage(RECIPE_ITEM_VALUES.get(itemId));
        }

        return BigInteger.ZERO;
    }

    private static String getMissingRecipeIngredientReason(String itemId) {
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            List<String> tagItems = TAG_ITEMS.get(tagId);
            if (tagItems == null || tagItems.isEmpty()) {
                return "Missing tag value";
            }

            boolean hasKnownAlternative = false;
            boolean hasBlockedAlternative = false;
            for (String tagItem : tagItems) {
                if (getRecipeIngredientValue(tagItem).signum() > 0) {
                    hasKnownAlternative = true;
                } else {
                    hasBlockedAlternative = true;
                }
            }

            if (!hasKnownAlternative && hasBlockedAlternative) {
                return "Tag alternatives all blocked";
            }

            return "Missing tag value";
        }

        if (recipeKeySearch(itemId)) {
            return "Blocked item recipe";
        }

        return "Missing item value";
    }

    public static List<String> getRecipeDebugLines(String itemId) {
        List<String> lines = new ArrayList<>();

        for (Map.Entry<String, List<String>> recipe : RECIPES.entrySet()) {
            String resultId = recipe.getKey().split("__")[0];
            if (!resultId.equals(itemId)) continue;

            BigInteger emc = combineEMC(recipe.getValue());
            lines.add("Recipe: " + RECIPE_SOURCES.getOrDefault(recipe.getKey(), recipe.getKey()));
            lines.add("Key: " + recipe.getKey());
            lines.add("Ingredients: " + formatIngredients(recipe.getValue()));
            lines.add("Ingredient EMC: " + (emc.signum() > 0 ? emc : "Blocked"));
            lines.add("Raw JSON:");
            lines.add(RECIPE_JSON.getOrDefault(recipe.getKey(), "Unavailable"));
            lines.add("");
        }

        if (lines.isEmpty()) {
            lines.add("No recipes found for " + itemId);
        }

        return lines;
    }

    public static boolean isConfigOverridden(String key) {
        return CONFIG_OVERRIDDEN.contains(key);
    }

    public static boolean hasExactValue(String key) {
        return EMC_VALUES.containsKey(key);
    }

    public static Set<String> getList() {
        return EMC_VALUES.keySet();
    }

    private static int syncVersion = 0;

    public static int getSyncVersion() {
        return syncVersion;
    }

    public static List<String> getSyncValues() {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, BigInteger> entry : EMC_VALUES.entrySet()) {
            values.add(entry.getKey() + "=" + entry.getValue());
        }

        return values;
    }

    public static int getLearnableCount() {
        if (!CLIENT_SYNC_VALUES.isEmpty()) return CLIENT_SYNC_VALUES.size();

        return EMC_VALUES.size();
    }

    public static void applyClientSyncValues(List<String> values) {
        CLIENT_SYNC_VALUES.clear();

        for (String value : values) {
            int splitIndex = value.lastIndexOf("=");
            if (splitIndex <= 0 || splitIndex >= value.length() - 1) continue;

            String itemId = value.substring(0, splitIndex);
            BigInteger emc;
            try {
                emc = EmcNumber.parse(value.substring(splitIndex + 1));
            } catch (NumberFormatException ignored) {
                continue;
            }

            CLIENT_SYNC_VALUES.put(itemId, emc);
        }
    }

    private static void incrementSyncVersion() {
        syncVersion++;
    }

    public static void init() {
        EmcValueSet values = ModConfig.DEFAULT_EMC_VALUES == null ? DefaultEmcValues.load() : ModConfig.DEFAULT_EMC_VALUES;

        String emcMode = ModConfig.MODE.toLowerCase();
        if (emcMode.contains("skyblock")) {
            values = values.applyOverride("skyblock");
        }

        if (ModConfig.CREATIVE_ITEMS) {
            values = values.applyOverride("creative_items");
        }

        values = values.applyValues(ModConfig.EMC_OVERRIDES);

        applyDefaultValues(values);
        loadConfig();
    }

    private static void applyDefaultValues(EmcValueSet values) {
        values.items().forEach((itemId, value) -> {
            if (value != null && value.signum() > 0) {
                setEMCUnchecked(itemId, value);
            } else {
                removeEMC(itemId);
            }
        });

        values.tags().forEach((tagId, value) -> {
            if (value != null && value.signum() > 0) {
                EMC_TAG_VALUES.put(tagId, value);
            } else {
                EMC_TAG_VALUES.remove(tagId);
            }
        });
    }

    private static void loadConfig() {
        if (ModConfig.EMC_OVERRIDES == null || (ModConfig.EMC_OVERRIDES.items().isEmpty() && ModConfig.EMC_OVERRIDES.tags().isEmpty())) {
            DissolverEnhanced.LOGGER.debug("No EMC overrides");
            return;
        }

        for (Map.Entry<String, BigInteger> emcOverride : ModConfig.EMC_OVERRIDES.items().entrySet()) {
            String blockName = emcOverride.getKey();
            BigInteger value = emcOverride.getValue();

            if (blockName == null) {
                continue;
            }

            blockName = blockName.trim();
            if (blockName.isEmpty()) {
                continue;
            }

            CONFIG_OVERRIDDEN.add(blockName);
            if (value != null && value.signum() > 0) {
                DissolverEnhanced.LOGGER.debug("Setting EMC of {} to {}", blockName, value);
                setEMCUnchecked(blockName, value, "EMC Override");
            } else {
                removeEMC(blockName);
            }
        }

        for (String tagId : ModConfig.EMC_OVERRIDES.tags().keySet()) {
            CONFIG_OVERRIDDEN.add("#" + tagId);
        }
    }

    private static void removeEMC(String blockName) {
        //remove if defined
        BigInteger rez = EMC_VALUES.remove(blockName);
        if (rez != null) {
            DissolverEnhanced.LOGGER.info("Removing EMC value from {}", blockName);
        }
        EMC_SOURCES.remove(blockName);
        EMC_SOURCE_DETAILS.remove(blockName);
    }

    private static void setEMCUnchecked(
        String blockName,
        BigInteger value
    ) {
        setEMCUnchecked(blockName, value, "Base Value");
    }

    private static void setEMCUnchecked(
        String blockName,
        BigInteger value,
        String source
    ) {
        setEMCUnchecked(blockName, value, source, "None");
    }

    private static void setEMCUnchecked(
        String blockName,
        BigInteger value,
        String source,
        String sourceDetail
    ) {
        //has value
        EMC_VALUES.put(blockName, value);
        EMC_SOURCES.put(blockName, source);
        EMC_SOURCE_DETAILS.put(blockName, sourceDetail);
    }

    protected static void setEMC(
        String blockName,
        int emcValue
    ) {
        setEMC(blockName, BigInteger.valueOf(emcValue));
    }

    protected static void setEMC(
        String blockName,
        BigInteger emcValue
    ) {
        if(CONFIG_OVERRIDDEN != null && CONFIG_OVERRIDDEN.contains(blockName)) {
            //config is locked
            return;
        }

        //TODO allow no overrides
        setEMCUnchecked(blockName, emcValue, "Generated Value");
    }

    protected static void setEMC(
        String blockName,
        int emcValue,
        String source
    ) {
        setEMC(blockName, BigInteger.valueOf(emcValue), source);
    }

    protected static void setEMC(
        String blockName,
        BigInteger emcValue,
        String source
    ) {
        if(CONFIG_OVERRIDDEN != null && CONFIG_OVERRIDDEN.contains(blockName)) {
            //config is locked
            return;
        }

        setEMCUnchecked(blockName, emcValue, source);
    }

    protected static void setEMC(
        String blockName,
        int emcValue,
        String source,
        String sourceDetail
    ) {
        setEMC(blockName, BigInteger.valueOf(emcValue), source, sourceDetail);
    }

    protected static void setEMC(
        String blockName,
        BigInteger emcValue,
        String source,
        String sourceDetail
    ) {
        if(CONFIG_OVERRIDDEN != null && CONFIG_OVERRIDDEN.contains(blockName)) {
            //config is locked
            return;
        }

        setEMCUnchecked(blockName, emcValue, source, sourceDetail);
    }

    protected static void setEMC(HashMap<String, BigInteger> NEW_EMC_VALUES) {
        if (NEW_EMC_VALUES == null || NEW_EMC_VALUES.isEmpty()) {
            return;
        }
        for (Map.Entry<String, BigInteger> entry : NEW_EMC_VALUES.entrySet()) {
            setEMC(entry.getKey(), entry.getValue(), "Tag");
        }
    }

    private static boolean tags_loaded = false;

    public static void tagsLoaded(HashMap<String, BigInteger> NEW_EMC_VALUES) {
        setEMC(NEW_EMC_VALUES);
        inferTagEMCValues();
        tags_loaded = true;

        if (tags_loaded && !RECIPES.isEmpty()) {startQuery();}
    }

    public static void tagsLoaded(
        HashMap<String, BigInteger> NEW_EMC_VALUES,
        HashMap<String, List<String>> newTagItems
    ) {
        if (newTagItems != null && !newTagItems.isEmpty()) {
            TAG_ITEMS.putAll(newTagItems);
        }

        tagsLoaded(NEW_EMC_VALUES);
    }

    private static void inferTagEMCValues() {
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Map.Entry<String, List<String>> tag : TAG_ITEMS.entrySet()) {
                String tagId = tag.getKey();
                if (EMC_TAG_VALUES.containsKey(tagId)) continue;

                BigInteger tagEMC = getEquivalentTagEMC(tag.getValue());
                if (tagEMC == null) continue;

                EMC_TAG_VALUES.put(tagId, tagEMC);
                changed = true;
            }
        }
    }

    private static BigInteger getEquivalentTagEMC(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return null;

        Set<BigInteger> knownValues = new HashSet<>();
        for (String itemId : itemIds) {
            BigInteger emc = getIngredientEMC(itemId);
            if (emc.signum() == 0) return null;
            knownValues.add(emc);
        }

        if (knownValues.size() != 1) return null;
        return knownValues.iterator().next();
    }

    private static HashMap<String, List<String>> RECIPES = new HashMap<String, List<String>>();
    private static HashMap<String, String> RECIPE_SOURCES = new HashMap<String, String>();
    private static HashMap<String, String> RECIPE_JSON = new HashMap<String, String>();
    private static List<String> STONE_CUTTER_LIST = new ArrayList<>();
    private static long startupStartedAt = 0;
    private static int startupRecipeCount = 0;
    private static int itemRecipesWithNoIngredients = 0;
    private static int recipesNotUnderstood = 0;
    private static int childParentUnmatchingEMC = 0;
    private static int itemsWithMultipleRecipes = 0;
    private static int itemsWithoutRecipeOrEMC = 0;
    private static int itemsWithoutEMC = 0;

    public static void recipesLoaded(
        HashMap<String, List<String>> recipes,
        List<String> stonecutter
    ) {
        RECIPES = recipes;
        STONE_CUTTER_LIST = stonecutter;

        if (tags_loaded && !RECIPES.isEmpty()) {startQuery();}
    }

    public static void recipesLoaded(
        HashMap<String, List<String>> recipes,
        HashMap<String, String> recipeSources,
        HashMap<String, String> recipeJson,
        List<String> stonecutter
    ) {
        RECIPE_SOURCES = recipeSources;
        RECIPE_JSON = recipeJson;
        recipesLoaded(recipes, stonecutter);
    }

    public static void beginStartup(int recipeCount) {
        startupStartedAt = System.currentTimeMillis();
        startupRecipeCount = recipeCount;
        itemRecipesWithNoIngredients = 0;
        recipesNotUnderstood = 0;
        childParentUnmatchingEMC = 0;
        itemsWithMultipleRecipes = 0;
        itemsWithoutRecipeOrEMC = 0;
        itemsWithoutEMC = 0;
        resetRecipeState();

        DissolverEnhanced.LOGGER.info("----- DissolverEnhanced initialized Startup - {} recipes -----", recipeCount);
    }

    public static void incrementItemRecipesWithNoIngredients() {
        itemRecipesWithNoIngredients++;
    }

    public static void incrementRecipesNotUnderstood() {
        recipesNotUnderstood++;
    }

    private static void startQuery() {
        queryRecipes(RECIPES);
    }

    private static List<String> unused = Arrays.asList(
        "minecraft:filled_map", "minecraft:tipped_arrow", "minecraft:debug_stick", "minecraft:small_amethyst_bud",
        "minecraft:large_amethyst_bud", "minecraft:disc_fragment_5", "minecraft:petrified_oak_slab",
        "minecraft:suspicious_stew", "minecraft:bundle", "minecraft:enchanted_book", "minecraft:air",
        "minecraft:ominous_bottle", "minecraft:structure_void", "minecraft:chipped_anvil", "minecraft:firework_star",
        "minecraft:knowledge_book", "minecraft:light", "minecraft:written_book", "minecraft:damaged_anvil",
        "minecraft:medium_amethyst_bud"
    );
    static int previousCompletedSize = 0;
    static int loops = 0;

    public static void queryRecipes(HashMap<String, List<String>> RECIPES) {
        loops++;
        for (Map.Entry<String, List<String>> recipe : RECIPES.entrySet()) {
            checkRecipe(recipe);
        }

        // brute force! (i'm sure there's a more optimized way)
        if (COMPLETED.size() != previousCompletedSize) {
            previousCompletedSize = COMPLETED.size();
            queryRecipes(RECIPES);
        } else {
            List<String> HAS_MULTIPLE = new ArrayList<>();
            RECIPE_ITEM_VALUES.forEach((resultId, emcValues) -> {
                BigInteger emcValue = getAverage(emcValues);
                // mostly stonecutter items!
                boolean ignored = resultId.contains("dye") || resultId.contains("copper") ||
                    resultId.contains("painting");
                if (emcValues.size() > 1 && !STONE_CUTTER_LIST.contains(resultId) && !HAS_MULTIPLE.contains(resultId) &&
                    !ignored) {
                    itemsWithMultipleRecipes++;
                    HAS_MULTIPLE.add(resultId);
                }
                setEMC(resultId, emcValue, "Recipe", getRecipeSourceDetail(resultId));

                // add dynamic
                if (resultId.contains("concrete_powder")) {
                    setEMC(resultId.substring(0, resultId.indexOf("_powder")), emcValue.add(BigInteger.valueOf(20)), "Recipe");
                }
            });

            inferEquivalentTagValues();

            // if (HAS_MULTIPLE.size() > 0) DissolverEnhanced.LOGGER.info("FOUND " + (HAS_MULTIPLE.size()) + " ITEMS WITH MULTIPLE DIFFERENT VALUES!");

            // LOG ITEMS WITH MISSING EMC - that does not have a crafting recipe!
            for (String missing : MISSING) {
                if (!COMPLETED.contains(missing) && !unused.contains(missing)) {
                    itemsWithoutRecipeOrEMC++;
                }
            }

            for (ResourceKey<Item> item : BuiltInRegistries.ITEM.registryKeySet()) {
                String itemId = item
                    .location()
                    .toString();
                if (!checkItem(itemId)) {itemsWithoutEMC++;}
            }

            logStartupSummary();
            incrementSyncVersion();
        }
    }

    private static List<String> creative_items = Arrays.asList(
        "spawn_egg", "command_block", "bedrock", "barrier", "structure_block", "jigsaw", "spawner", "vault",
        "end_portal_frame", "budding_amethyst", "reinforced_deepslate"
    );

    private static boolean checkItem(String itemId) {
        // add dynamic (creative items)
        if (ModConfig.CREATIVE_ITEMS) {
            if (itemId.contains("spawn_egg")) {
                setEMC(itemId, 100000);
            }
            return true;
        }

        for (String itemPart : creative_items) {
            if (itemId.contains(itemPart)) {return true;}
        }

        if (!EMC_VALUES.containsKey(itemId) && !unused.contains(itemId) && !itemId.contains("bucket") &&
            !itemId.contains("potion") && !itemId.contains("infested_") && itemId != "minecraft:air") {
            return false;
        }

        return true;
    }

    private static BigInteger getAverage(List<BigInteger> list) {
        if (list == null || list.isEmpty()) return BigInteger.ZERO;

        BigInteger total = BigInteger.ZERO;
        for (BigInteger value : list) {
            total = total.add(EmcNumber.nonNegative(value));
        }
        return total.divide(BigInteger.valueOf(list.size()));
    }

    private static void inferEquivalentTagValues() {
        for (Map.Entry<String, List<String>> tag : TAG_ITEMS.entrySet()) {
            String tagId = tag.getKey();

            Set<BigInteger> knownValues = new HashSet<>();
            for (String itemId : tag.getValue()) {
                BigInteger emc = getBig(itemId);
                if (emc.signum() > 0) {
                    knownValues.add(emc);
                }
            }

            if (knownValues.size() != 1) {
                if (knownValues.size() > 1) {
                    DissolverEnhanced.LOGGER.debug("Skipping equivalent EMC tag #{} because it has mixed values: {}", tagId, knownValues);
                }
                continue;
            }

            BigInteger equivalentEMC = knownValues.iterator().next();
            for (String itemId : tag.getValue()) {
                if (getBig(itemId).signum() == 0 && !recipeKeySearch(itemId)) {
                    setEMC(itemId, equivalentEMC, "Equivalent Tag #" + tagId);
                }
            }
        }
    }

    private static final List<String> COMPLETED = new ArrayList<String>();
    private static final List<String> MISSING = new ArrayList<String>();
    private static final HashMap<String, List<BigInteger>> RECIPE_ITEM_VALUES = new HashMap<String, List<BigInteger>>();
    private static final HashMap<String, List<String>> RECIPE_ITEM_SOURCE_DETAILS = new HashMap<String, List<String>>();
    private static final HashMap<String, List<String>> PARENTS = new HashMap<String, List<String>>();

    private static void resetRecipeState() {
        COMPLETED.clear();
        MISSING.clear();
        RECIPE_ITEM_VALUES.clear();
        RECIPE_ITEM_SOURCE_DETAILS.clear();
        PARENTS.clear();
        previousCompletedSize = 0;
        loops = 0;
    }

    private static void checkRecipe(Map.Entry<String, List<String>> recipe) {
        String id = recipe.getKey();
        if (COMPLETED.contains(id)) {return;}

        String[] parts = id.split("__");
        String resultId = parts[0];
        int resultCount = Integer.parseInt(parts[1]);
        BigInteger extraEMC = EmcNumber.parse(parts[2]); // cooking

        List<String> ingredients = recipe.getValue();
        BigInteger totalInputEMC = combineEMC(ingredients);
        if (totalInputEMC.signum() == 0) {
            if (checkReverseRecipe(id, resultId, resultCount, extraEMC, ingredients)) {
                COMPLETED.add(id);
            }
            return; // try again!
        }

        if (EMC_VALUES.containsKey(resultId) && !RECIPE_ITEM_VALUES.containsKey(resultId)) {
            COMPLETED.add(id);
            return;
        }

        COMPLETED.add(id);

        // don't allow "children" to change the item they received emc value from
        if (PARENTS.containsKey(resultId)) {
            if (!RECIPE_ITEM_VALUES.containsKey(resultId) || resultId.contains("copper")) {return;}

            // check if emc values are different
            BigInteger previousEMC = RECIPE_ITEM_VALUES
                .get(resultId)
                .get(0);
            BigInteger newEMC = totalInputEMC.divide(BigInteger.valueOf(resultCount)).add(extraEMC);
            // round up to 1 if below 1
            if (newEMC.signum() < 1) {newEMC = BigInteger.ONE;}
            if (previousEMC.equals(newEMC)) {return;}

            childParentUnmatchingEMC++;
            return;
        } else if (ingredients.size() == 1) {
            List<String> children = new ArrayList<>();
            String parentId = ingredients.get(0);
            if (PARENTS.containsKey(parentId)) {children = PARENTS.get(parentId);}
            children.add(resultId);
            PARENTS.put(parentId, children);
        }

        List<BigInteger> values = new ArrayList<>();
        if (RECIPE_ITEM_VALUES.containsKey(resultId)) {values = RECIPE_ITEM_VALUES.get(resultId);}

        totalInputEMC = totalInputEMC.divide(BigInteger.valueOf(resultCount)).add(extraEMC); // divide value on output item count
        // round up to 1 if below 1
        if (totalInputEMC.signum() < 1) {totalInputEMC = BigInteger.ONE;}

        if (values.contains(totalInputEMC)) {
            return; // same value
        }
        values.add(totalInputEMC);

        RECIPE_ITEM_VALUES.put(resultId, values);
        addRecipeSourceDetail(resultId, id, totalInputEMC, ingredients, resultCount, extraEMC);
    }

    private static void logStartupSummary() {
        long elapsedMs = startupStartedAt == 0 ? 0 : System.currentTimeMillis() - startupStartedAt;

        DissolverEnhanced.LOGGER.info(
            String.join("\n",
                "----- DissolverEnhanced Finished Startup -----",
                "Time to finish: " + elapsedMs + "ms",
                "Recipes scanned: " + startupRecipeCount,
                "Recipe loops: " + loops,
                "Item Recipes with no ingredients: " + itemRecipesWithNoIngredients,
                "Recipes not understood: " + recipesNotUnderstood,
                "Child & Parent with unmatching EMC: " + childParentUnmatchingEMC,
                "Items with multiple different recipes: " + itemsWithMultipleRecipes,
                "Items without Recipe or EMC: " + itemsWithoutRecipeOrEMC,
                "Items without EMC: " + itemsWithoutEMC,
                "Items set with EMC: " + EMC_VALUES.size()
            )
        );
    }

    private static boolean checkReverseRecipe(
        String recipeKey,
        String resultId,
        int resultCount,
        BigInteger extraEMC,
        List<String> ingredients
    ) {
        if (extraEMC.signum() != 0 || ingredients.isEmpty()) return false;

        String unknownIngredient = ingredients.get(0);
        for (String ingredient : ingredients) {
            if (!ingredient.equals(unknownIngredient) || getBig(ingredient).signum() > 0) {
                return false;
            }
        }

        BigInteger resultEMC = getResultEMC(resultId);
        if (resultEMC.signum() == 0) return false;

        BigInteger totalResultEMC = resultEMC.multiply(BigInteger.valueOf(resultCount));
        BigInteger divisor = BigInteger.valueOf(ingredients.size());
        BigInteger ingredientEMC = totalResultEMC.add(divisor).subtract(BigInteger.ONE).divide(divisor);
        if (ingredientEMC.signum() < 1) ingredientEMC = BigInteger.ONE;

        setEMC(
            unknownIngredient,
            ingredientEMC,
            "Reverse Recipe",
            formatReverseRecipeSourceDetail(recipeKey, resultId, resultCount, ingredients, ingredientEMC)
        );
        return true;
    }

    private static BigInteger getResultEMC(String resultId) {
        BigInteger resultEMC = getBig(resultId);
        if (resultEMC.signum() > 0) return resultEMC;

        if (RECIPE_ITEM_VALUES.containsKey(resultId)) {
            return getAverage(RECIPE_ITEM_VALUES.get(resultId));
        }

        return BigInteger.ZERO;
    }

    private static void addRecipeSourceDetail(
        String resultId,
        String recipeKey,
        BigInteger emcValue,
        List<String> ingredients,
        int resultCount,
        BigInteger extraEMC
    ) {
        List<String> sourceDetails = new ArrayList<>();
        if (RECIPE_ITEM_SOURCE_DETAILS.containsKey(resultId)) {
            sourceDetails = RECIPE_ITEM_SOURCE_DETAILS.get(resultId);
        }

        String sourceDetail = formatRecipeSourceDetail(recipeKey, resultId, resultCount, ingredients, emcValue, extraEMC);
        if (!sourceDetails.contains(sourceDetail)) {
            sourceDetails.add(sourceDetail);
        }

        RECIPE_ITEM_SOURCE_DETAILS.put(resultId, sourceDetails);
    }

    private static String getRecipeSourceDetail(String resultId) {
        if (!RECIPE_ITEM_SOURCE_DETAILS.containsKey(resultId)) {
            return "None";
        }

        return String.join("; ", RECIPE_ITEM_SOURCE_DETAILS.get(resultId));
    }

    private static String formatRecipeSourceDetail(
        String recipeKey,
        String resultId,
        int resultCount,
        List<String> ingredients,
        BigInteger emcValue,
        BigInteger extraEMC
    ) {
        return "Recipe " + getRecipeSource(recipeKey) + ": " + formatIngredients(ingredients) + " -> " +
            resultCount + "x " + resultId + " = " + emcValue + " EMC" +
            (extraEMC.signum() > 0 ? " (+" + extraEMC + " cooking)" : "");
    }

    private static String formatReverseRecipeSourceDetail(
        String recipeKey,
        String resultId,
        int resultCount,
        List<String> ingredients,
        BigInteger emcValue
    ) {
        return "Reverse recipe " + getRecipeSource(recipeKey) + ": " + formatIngredients(ingredients) + " -> " +
            resultCount + "x " + resultId + " = " + emcValue + " EMC each";
    }

    private static String getRecipeSource(String recipeKey) {
        return RECIPE_SOURCES.getOrDefault(recipeKey, recipeKey);
    }

    private static String formatIngredients(List<String> ingredients) {
        HashMap<String, Integer> counts = new HashMap<>();
        for (String ingredient : ingredients) {
            counts.put(ingredient, counts.getOrDefault(ingredient, 0) + 1);
        }

        List<String> formatted = new ArrayList<>();
        counts.forEach((itemId, count) -> formatted.add(count + "x " + itemId));
        return String.join(", ", formatted);
    }

    private static BigInteger combineEMC(List<String> itemIds) {
        BigInteger totalEmcValue = BigInteger.ZERO;

        for (String itemId : itemIds) {
            BigInteger emcValue = getIngredientEMC(itemId);
            if (emcValue.signum() == 0) {
                // could not get value for all blocks
                if (!RECIPE_ITEM_VALUES.containsKey(itemId)) {
                    if (!recipeKeySearch(itemId) && !MISSING.contains(itemId)) {MISSING.add(itemId);}
                    return BigInteger.ZERO;
                }

                if (MISSING.contains(itemId)) {MISSING.remove(itemId);}
                emcValue = getAverage(RECIPE_ITEM_VALUES.get(itemId));
            }

            totalEmcValue = totalEmcValue.add(emcValue);
        }

        return totalEmcValue;
    }

    private static BigInteger getIngredientEMC(String itemId) {
        if (itemId.startsWith("#")) {
            String tagId = itemId.substring(1);
            BigInteger tagEMC = EMC_TAG_VALUES.getOrDefault(tagId, BigInteger.ZERO);
            if (tagEMC.signum() > 0) return tagEMC;

            BigInteger inferredEMC = getEquivalentTagIngredientEMC(TAG_ITEMS.get(tagId));
            return inferredEMC == null ? BigInteger.ZERO : inferredEMC;
        }

        return getBig(itemId);
    }

    private static BigInteger getEquivalentTagIngredientEMC(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }

        Set<BigInteger> knownValues = new HashSet<>();
        for (String itemId : itemIds) {
            BigInteger emc = getResultEMC(itemId);
            if (emc.signum() > 0) {
                knownValues.add(emc);
            }
        }

        if (knownValues.size() != 1) {
            return null;
        }

        return knownValues.iterator().next();
    }

    static private boolean recipeKeySearch(String keyId) {
        for (String key : RECIPES.keySet()) {
            if (key.contains(keyId)) {
                return true; // keyId__{outputCount}__{extraEMC}__{craftingIndex}
            }
        }

        return false;
    }
}
