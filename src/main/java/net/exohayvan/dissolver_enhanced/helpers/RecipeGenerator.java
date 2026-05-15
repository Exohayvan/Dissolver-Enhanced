package net.exohayvan.dissolver_enhanced.helpers;

import java.util.ArrayList;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.minecraft.resources.ResourceLocation;

public class RecipeGenerator {
    public static JsonObject DISSOLVER_RECIPE;

    public static void init() {
        String craftingDifficulty = ModConfig.DIFFICULTY.toLowerCase();
        // hard
        ResourceLocation frameItem = ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "crystal_frame_item");
        ResourceLocation centerItem = ResourceLocation.fromNamespaceAndPath("minecraft", "nether_star");

        if (craftingDifficulty.contains("easy")) {
            frameItem = ResourceLocation.fromNamespaceAndPath("minecraft", "glass_pane");
            centerItem = ResourceLocation.fromNamespaceAndPath("minecraft", "redstone");
        } else if (craftingDifficulty.contains("normal")) {
            centerItem = ResourceLocation.fromNamespaceAndPath("minecraft", "phantom_membrane");
        }

        DISSOLVER_RECIPE = createShapedRecipeJson(
            Lists.newArrayList('C', '#'),
            Lists.newArrayList(frameItem, centerItem),
            Lists.newArrayList("item", "item"),
            Lists.newArrayList(
                "CCC",
                "C#C",
                "CCC"
            ),
            ResourceLocation.fromNamespaceAndPath(DissolverEnhanced.MOD_ID, "dissolver_block")
        );
    }

    // source: https://fabricmc.net/wiki/tutorial:dynamic_recipe_generation
    public static JsonObject createShapedRecipeJson(ArrayList<Character> keys, ArrayList<ResourceLocation> items, ArrayList<String> type, ArrayList<String> pattern, ResourceLocation output) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(pattern.get(0));
        jsonArray.add(pattern.get(1));
        jsonArray.add(pattern.get(2));
        json.add("pattern", jsonArray);

        JsonObject individualKey;
        JsonObject keyList = new JsonObject();

        for (int i = 0; i < keys.size(); ++i) {
            individualKey = new JsonObject();
            individualKey.addProperty(type.get(i), items.get(i).toString());
            keyList.add(keys.get(i) + "", individualKey);
        }

        json.add("key", keyList);

        JsonObject result = new JsonObject();
        result.addProperty("id", output.toString());
        result.addProperty("count", 1);
        json.add("result", result);

        return json;
    }

}
