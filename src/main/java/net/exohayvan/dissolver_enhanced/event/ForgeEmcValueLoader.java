package net.exohayvan.dissolver_enhanced.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.data.RecipeLoadCoordinator;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
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
        RecipeLoadCoordinator.GLOBAL.runExclusive(() -> loadRecipesLocked(server));
    }

    private static void loadRecipesLocked(MinecraftServer server) {
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
                DissolverEnhanced.LOGGER.debug("Could not read recipe {} for EMC calculation.", recipeId(recipe), exception);
            }
        }

        EMCValues.recipesLoaded(RECIPES, RECIPE_SOURCES, RECIPE_JSON, STONE_CUTTER_LIST);
    }

    private static void addRecipe(MinecraftServer server, RecipeHolder<?> recipeHolder) {
        ResourceLocation recipeId = recipeId(recipeHolder);
        Recipe<?> recipe = recipeHolder.value();
        RecipeType<?> recipeType = recipe.getType();

        ItemStack resultItem = resultItem(server, recipe);
        String resultId = ItemHelper.getId(resultItem.getItem());
        int resultCount = resultItem.getCount();

        if (resultId.contains("minecraft:air") || resultId.contains("firework")) return;
        boolean isCooking = recipeType == RecipeType.SMELTING || recipeType == RecipeType.BLASTING ||
            recipeType == RecipeType.SMOKING || recipeType == RecipeType.CAMPFIRE_COOKING;
        if (isCooking && resultId.contains("nugget")) return;

        List<String> ingredients = new ArrayList<>();
        HashMap<String, List<String>> replaceIngredients = new HashMap<>();
        boolean hasUnresolvedIngredient = false;

        for (Ingredient ingredient : ingredients(recipe)) {
            List<ItemStack> ingredientItems = ingredientItems(ingredient);
            if (ingredientItems.isEmpty()) {
                continue;
            }

            int index = -1;
            String rootItemId = null;

            for (ItemStack stack : ingredientItems) {
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

    private static ResourceLocation recipeId(RecipeHolder<?> recipeHolder) {
        try {
            Method method = RecipeHolder.class.getMethod("id");
            Object id = method.invoke(recipeHolder);
            if (id instanceof ResourceLocation location) {
                return location;
            }
            if (id instanceof net.minecraft.resources.ResourceKey<?> key) {
                return key.location();
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            DissolverEnhanced.LOGGER.debug("Could not reflect recipe id.", exception);
        }

        return ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "unknown_recipe");
    }

    private static ItemStack resultItem(MinecraftServer server, Recipe<?> recipe) {
        ItemStack oldResult = invokeItemStack(recipe, "getResultItem", new Class<?>[] { net.minecraft.core.HolderLookup.Provider.class }, server.registryAccess());
        if (oldResult != null) {
            return oldResult;
        }

        try {
            Method displayMethod = Recipe.class.getMethod("display");
            Object displays = displayMethod.invoke(recipe);
            if (displays instanceof Iterable<?> iterable) {
                for (Object display : iterable) {
                    Method resultMethod = display.getClass().getMethod("result");
                    ItemStack stack = slotDisplayItemStack(resultMethod.invoke(display));
                    if (stack != null && !stack.isEmpty()) {
                        return stack;
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            DissolverEnhanced.LOGGER.debug("Could not reflect recipe display result.", exception);
        }

        return ItemStack.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private static List<Ingredient> ingredients(Recipe<?> recipe) {
        try {
            Method method = Recipe.class.getMethod("getIngredients");
            Object ingredients = method.invoke(recipe);
            if (ingredients instanceof List<?> list) {
                return (List<Ingredient>)list;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Minecraft 1.21.2 moved recipe ingredients to PlacementInfo.
        }

        try {
            Method placementInfoMethod = Recipe.class.getMethod("placementInfo");
            Object placementInfo = placementInfoMethod.invoke(recipe);
            Method ingredientsMethod = placementInfo.getClass().getMethod("ingredients");
            Object ingredients = ingredientsMethod.invoke(placementInfo);
            if (ingredients instanceof List<?> list) {
                return (List<Ingredient>)list;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            DissolverEnhanced.LOGGER.debug("Could not reflect recipe ingredients.", exception);
        }

        return List.of();
    }

    private static List<ItemStack> ingredientItems(Ingredient ingredient) {
        try {
            Method method = Ingredient.class.getMethod("getItems");
            Object items = method.invoke(ingredient);
            if (items instanceof ItemStack[] stacks) {
                return List.of(stacks);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Minecraft 1.21.2 exposes holders through Ingredient#items().
        }

        try {
            Method method = Ingredient.class.getMethod("items");
            Object items = method.invoke(ingredient);
            if (items instanceof Iterable<?> iterable) {
                List<ItemStack> stacks = new ArrayList<>();
                for (Object item : iterable) {
                    if (item instanceof Holder<?> holder && holder.value() instanceof Item heldItem) {
                        stacks.add(new ItemStack(heldItem));
                    }
                }
                return stacks;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            DissolverEnhanced.LOGGER.debug("Could not reflect ingredient items.", exception);
        }

        return List.of();
    }

    private static ItemStack slotDisplayItemStack(Object slotDisplay) {
        try {
            Method itemMethod = slotDisplay.getClass().getMethod("item");
            Object item = itemMethod.invoke(slotDisplay);
            if (item instanceof Holder<?> holder && holder.value() instanceof Item heldItem) {
                return new ItemStack(heldItem);
            }
            if (item instanceof Item heldItem) {
                return new ItemStack(heldItem);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not every SlotDisplay is a simple item display.
        }

        return invokeItemStack(slotDisplay, "resolveForFirstStack", new Class<?>[] { contextMapClass() }, new Object[] { null });
    }

    private static ItemStack invokeItemStack(Object target, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(target, arguments);
            if (result instanceof ItemStack stack) {
                return stack;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }

        return null;
    }

    private static Class<?> contextMapClass() {
        try {
            return Class.forName("net.minecraft.util.context.ContextMap");
        } catch (ClassNotFoundException exception) {
            return Object.class;
        }
    }

    private static boolean listSearch(List<String> ingredients, String keyId) {
        for (String key : ingredients) {
            if (key.contains(keyId)) return true;
        }

        return false;
    }
}
