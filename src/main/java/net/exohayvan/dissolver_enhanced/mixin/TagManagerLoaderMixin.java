package net.exohayvan.dissolver_enhanced.mixin;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagManager;
import net.minecraft.tags.TagManager.LoadResult;

@Mixin(TagManager.class)
public abstract class TagManagerLoaderMixin {
    private static final HashMap<String, BigInteger> NEW_EMC_VALUES = new HashMap<String, BigInteger>();
    private static final HashMap<String, List<String>> TAG_ITEMS = new HashMap<String, List<String>>();

    @Inject(method = "getResult", at = @At("RETURN"))
    private void getResult(CallbackInfoReturnable<List<LoadResult<?>>> cir) {
        // TAG TYPE LIST (1.21): minecraft:banner_pattern, minecraft:worldgen/structure, minecraft:entity_type, minecraft:worldgen/world_preset, minecraft:fluid, minecraft:cat_variant, minecraft:game_event, minecraft:instrument, minecraft:item, minecraft:worldgen/biome, minecraft:enchantment, minecraft:block, minecraft:worldgen/flat_level_generator_preset, minecraft:painting_variant, minecraft:point_of_interest_type, minecraft:damage_type
        // List<String> tagTypeList = new ArrayList<>();
        NEW_EMC_VALUES.clear();
        TAG_ITEMS.clear();

        cir.getReturnValue().forEach(tagSet -> {
            for (Map.Entry<ResourceLocation, ?> tag : tagSet.tags().entrySet()) {
                ResourceLocation tagId = tag.getKey();
                Collection<?> value = (Collection<?>)tag.getValue();

                List<String> itemIds = new ArrayList<>();
                value.forEach(tagValue -> {
                    Holder<?> ref = (Holder<?>)tagValue;
                    ResourceKey<?> key = ref.unwrapKey().orElse(null);
                    if (key == null) return;

                    ResourceLocation tagTypeId = key.registry();
                    
                    // if (!tagTypeList.contains(tagTypeId.toString())) tagTypeList.add(tagTypeId.toString());
                    if (!tagTypeId.toString().contains("block") && !tagTypeId.toString().contains("item")) return;

                    ResourceLocation itemId = key.location();
                    itemIds.add(itemId.toString());
                });

                if (itemIds.size() > 0) checkTagForEMC(tagId, itemIds);
            }
        });

        EMCValues.tagsLoaded(NEW_EMC_VALUES, TAG_ITEMS);

        // DissolverEnhanced.LOGGER.info("TAG TYPE LIST: " + tagTypeList);
    }

    private void checkTagForEMC(ResourceLocation tagId, List<String> itemIds) {
        String tagIdString = tagId.toString();
        TAG_ITEMS.put(tagIdString, itemIds);

        BigInteger emcValue = getTagEmcValue(tagIdString);
        if (emcValue == null) {
            // this will also log tags unrelated to crafting
            // String firstItem = itemIds.get(0);
            // DissolverEnhanced.LOGGER.info("FOUND TAG WITH NO EMC: " + tagId + " ITEM: " + firstItem);
            return;
        }

        itemIds.forEach((itemId) -> {
            NEW_EMC_VALUES.put(itemId, emcValue);
        });
    }

    private BigInteger getTagEmcValue(String tagIdString) {
        BigInteger value = EMCValues.EMC_TAG_VALUES.get(tagIdString);
        if (value != null) return value;

        if (tagIdString.startsWith("forge:")) {
            return EMCValues.EMC_TAG_VALUES.get("c:" + tagIdString.substring("forge:".length()));
        }

        if (tagIdString.startsWith("c:")) {
            return EMCValues.EMC_TAG_VALUES.get("forge:" + tagIdString.substring("c:".length()));
        }

        return null;
    }
}
