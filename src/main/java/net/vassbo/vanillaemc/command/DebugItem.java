package net.vassbo.vanillaemc.command;

import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.vassbo.vanillaemc.VanillaEMC;
import net.vassbo.vanillaemc.data.EMCValues;

public class DebugItem {
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static int summary(CommandContext<ServerCommandSource> context, String command) {
        List<Item> items = Registries.ITEM
            .stream()
            .filter(item -> !item.getDefaultStack().isEmpty())
            .toList();

        int totalItems = items.size();
        int itemsWithEMC = 0;
        int itemsWithoutEMC = 0;
        HashMap<String, Integer> missingTagCounts = new HashMap<>();

        for (Item item : items) {
            String itemId = item.toString();
            int emc = EMCValues.get(itemId);

            if (emc > 0) {
                itemsWithEMC++;
                continue;
            }

            itemsWithoutEMC++;
            item.getDefaultStack()
                .streamTags()
                .map(TagKey::id)
                .map(Identifier::toString)
                .filter(tagId -> !EMCValues.EMC_TAG_VALUES.containsKey(tagId))
                .forEach(tagId -> missingTagCounts.put(tagId, missingTagCounts.getOrDefault(tagId, 0) + 1));
        }

        List<Map.Entry<String, Integer>> tagsWithoutValues = missingTagCounts
            .entrySet()
            .stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .toList();
        List<Map.Entry<String, Integer>> topTags = tagsWithoutValues
            .stream()
            .limit(5)
            .toList();
        List<Map.Entry<String, EMCValues.RecipeUnlockInfo>> recipeUnlockItems = EMCValues.getRecipeUnlockInfos()
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue().count(), a.getValue().count()))
            .toList();
        List<Map.Entry<String, EMCValues.RecipeUnlockInfo>> topRecipeUnlockItems = recipeUnlockItems
            .stream()
            .limit(5)
            .toList();

        double percentWithEMC = totalItems == 0 ? 0 : (itemsWithEMC * 100.0) / totalItems;
        List<String> lines = new ArrayList<>();
        lines.add("Total EMC Debug");
        lines.add("Total Items: " + totalItems);
        lines.add("Items with EMC: " + itemsWithEMC);
        lines.add("Items without EMC: " + itemsWithoutEMC);
        lines.add("% of items with EMC: " + formatPercent(percentWithEMC));
        lines.add("Top 5 tags without values:");

        if (topTags.isEmpty()) {
            lines.add("None");
        } else {
            for (Map.Entry<String, Integer> tag : topTags) {
                double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (tag.getValue() * 100.0) / itemsWithoutEMC;
                lines.add("#" + tag.getKey() + " - " + formatPercent(percentOfMissingItems));
            }
        }
        lines.add("Top 5 missing recipe ingredients:");

        if (topRecipeUnlockItems.isEmpty()) {
            lines.add("None");
        } else {
            for (Map.Entry<String, EMCValues.RecipeUnlockInfo> item : topRecipeUnlockItems) {
                lines.add(formatRecipeUnlockItem(item, itemsWithoutEMC));
            }
        }

        Path reportPath = writeReport(lines, tagsWithoutValues, recipeUnlockItems, itemsWithoutEMC);
        lines.add("Report: " + reportPath);

        String debugText = String.join("\n", lines);
        VanillaEMC.LOGGER.info(debugText);
        ModCommands.feedback(context, debugText);
        return 1;
    }

    public static int namespace(CommandContext<ServerCommandSource> context, String command) {
        return namespaceSummary(context, null);
    }

    public static int namespaceFiltered(CommandContext<ServerCommandSource> context, String command) {
        return namespaceSummary(context, StringArgumentType.getString(context, "namespace"));
    }

    private static int namespaceSummary(CommandContext<ServerCommandSource> context, String namespace) {
        List<Item> items = Registries.ITEM
            .stream()
            .filter(item -> !item.getDefaultStack().isEmpty())
            .filter(item -> namespace == null || getNamespace(item.toString()).equals(namespace))
            .toList();

        int totalItems = items.size();
        int itemsWithEMC = 0;
        int itemsWithoutEMC = 0;
        HashMap<String, Integer> missingNamespaceCounts = new HashMap<>();
        HashMap<String, List<String>> missingItemsByNamespace = new HashMap<>();
        HashMap<String, Integer> missingTagCounts = new HashMap<>();

        for (Item item : items) {
            String itemId = item.toString();
            int emc = EMCValues.get(itemId);

            if (emc > 0) {
                itemsWithEMC++;
                continue;
            }

            itemsWithoutEMC++;
            String itemNamespace = getNamespace(itemId);
            missingNamespaceCounts.put(itemNamespace, missingNamespaceCounts.getOrDefault(itemNamespace, 0) + 1);

            List<String> missingItems = new ArrayList<>();
            if (missingItemsByNamespace.containsKey(itemNamespace)) {
                missingItems = missingItemsByNamespace.get(itemNamespace);
            }
            missingItems.add(itemId);
            missingItemsByNamespace.put(itemNamespace, missingItems);

            item.getDefaultStack()
                .streamTags()
                .map(TagKey::id)
                .map(Identifier::toString)
                .filter(tagId -> !EMCValues.EMC_TAG_VALUES.containsKey(tagId))
                .forEach(tagId -> missingTagCounts.put(tagId, missingTagCounts.getOrDefault(tagId, 0) + 1));
        }

        List<Map.Entry<String, Integer>> namespacesWithMissingItems = missingNamespaceCounts
            .entrySet()
            .stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .toList();
        List<Map.Entry<String, Integer>> topNamespaces = namespacesWithMissingItems
            .stream()
            .limit(5)
            .toList();
        List<Map.Entry<String, Integer>> tagsWithoutValues = missingTagCounts
            .entrySet()
            .stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .toList();
        List<Map.Entry<String, EMCValues.RecipeUnlockInfo>> recipeUnlockItems = EMCValues.getRecipeUnlockInfos(namespace)
            .entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue().count(), a.getValue().count()))
            .toList();

        double percentWithEMC = totalItems == 0 ? 0 : (itemsWithEMC * 100.0) / totalItems;
        List<String> lines = new ArrayList<>();
        lines.add(namespace == null ? "Namespace EMC Debug" : "Namespace EMC Debug: " + namespace);
        lines.add("Total Items: " + totalItems);
        lines.add("Items with EMC: " + itemsWithEMC);
        lines.add("Items without EMC: " + itemsWithoutEMC);
        lines.add("% of items with EMC: " + formatPercent(percentWithEMC));
        lines.add("Top 5 namespaces with missing EMC:");

        if (topNamespaces.isEmpty()) {
            lines.add("None");
        } else {
            for (Map.Entry<String, Integer> itemNamespace : topNamespaces) {
                double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (itemNamespace.getValue() * 100.0) / itemsWithoutEMC;
                lines.add(itemNamespace.getKey() + " - " + formatPercent(percentOfMissingItems) + " (" + itemNamespace.getValue() + " items)");
            }
        }

        if (namespace != null) {
            lines.add("Top 5 tags without values:");
            if (tagsWithoutValues.isEmpty()) {
                lines.add("None");
            } else {
                for (Map.Entry<String, Integer> tag : tagsWithoutValues.stream().limit(5).toList()) {
                    double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (tag.getValue() * 100.0) / itemsWithoutEMC;
                    lines.add("#" + tag.getKey() + " - " + formatPercent(percentOfMissingItems));
                }
            }

            lines.add("Top 5 missing recipe ingredients:");
            if (recipeUnlockItems.isEmpty()) {
                lines.add("None");
            } else {
                for (Map.Entry<String, EMCValues.RecipeUnlockInfo> item : recipeUnlockItems.stream().limit(5).toList()) {
                    lines.add(formatRecipeUnlockItem(item, itemsWithoutEMC));
                }
            }
        }

        Path reportPath = writeNamespaceReport(lines, namespace, namespacesWithMissingItems, missingItemsByNamespace, tagsWithoutValues, recipeUnlockItems, itemsWithoutEMC);
        lines.add("Report: " + reportPath);

        String debugText = String.join("\n", lines);
        VanillaEMC.LOGGER.info(debugText);
        ModCommands.feedback(context, debugText);
        return 1;
    }

    public static int item(CommandContext<ServerCommandSource> context, String command) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            ModCommands.feedback(context, "This command must be run by a player.");
            return 0;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            ModCommands.feedback(context, "Hold an item to debug it.");
            return 0;
        }

        String itemId = stack.getItem().toString();
        int emc = EMCValues.get(itemId);
        String emcText = emc > 0 ? String.valueOf(emc) : "None";
        String stackEmcText = emc > 0 ? String.valueOf(emc * stack.getCount()) : "None";
        boolean configOverride = EMCValues.isConfigOverridden(itemId);

        List<TagKey<Item>> tags = stack.streamTags().toList();
        String debugText = String.join("\n",
            "Name: " + stack.getName().getString(),
            "ID: " + itemId,
            "Count: " + stack.getCount(),
            "Minecraft Tags: " + formatTags(tags, "minecraft"),
            "Common Tags: " + formatTags(tags, "c"),
            "Other Tags: " + formatOtherTags(tags),
            "EMC: " + emcText,
            "Stack EMC: " + stackEmcText,
            "EMC Source: " + EMCValues.getSource(itemId),
            "EMC Source Detail: " + EMCValues.getSourceDetail(itemId),
            "Config Override: " + (configOverride ? "Yes" : "No"),
            "Has Components: " + (!stack.getComponentChanges().isEmpty() ? "Yes" : "No"),
            "Learnable: " + (emc > 0 ? "Yes" : "No")
        );

        VanillaEMC.LOGGER.info(debugText);
        ModCommands.feedback(context, debugText);
        return 1;
    }

    public static int recipe(CommandContext<ServerCommandSource> context, String command) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            ModCommands.feedback(context, "This command must be run by a player.");
            return 0;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            ModCommands.feedback(context, "Hold an item to debug its recipes.");
            return 0;
        }

        String itemId = stack.getItem().toString();
        List<String> recipeLines = EMCValues.getRecipeDebugLines(itemId);
        Path reportPath = writeRecipeReport(itemId, recipeLines);

        String chatText = String.join("\n",
            "Recipe Debug: " + itemId,
            "Recipes found: " + countRecipes(recipeLines),
            "Report: " + reportPath
        );
        String logText = "Recipe Debug: " + itemId + "\n" + String.join("\n", recipeLines);

        VanillaEMC.LOGGER.info(logText);
        ModCommands.feedback(context, chatText);
        return 1;
    }

    private static String formatTags(List<TagKey<Item>> tags, String namespace) {
        String result = tags
            .stream()
            .map(TagKey::id)
            .filter(id -> id.getNamespace().equals(namespace))
            .map(DebugItem::formatTag)
            .collect(joining(", "));

        return result.isEmpty() ? "None" : result;
    }

    private static String formatOtherTags(List<TagKey<Item>> tags) {
        String result = tags
            .stream()
            .map(TagKey::id)
            .filter(id -> !id.getNamespace().equals("minecraft") && !id.getNamespace().equals("c"))
            .map(DebugItem::formatTag)
            .collect(joining(", "));

        return result.isEmpty() ? "None" : result;
    }

    private static String formatTag(Identifier id) {
        return "#" + id;
    }

    private static String formatPercent(double value) {
        return String.format("%.2f%%", value);
    }

    private static Path writeReport(
        List<String> lines,
        List<Map.Entry<String, Integer>> tagsWithoutValues,
        List<Map.Entry<String, EMCValues.RecipeUnlockInfo>> recipeUnlockItems,
        int itemsWithoutEMC
    ) {
        Path reportDir = Path.of("debug");
        String fileName = "dissolver-debug-report-" + LocalDateTime.now().format(REPORT_DATE_FORMAT);
        Path reportPath = reportDir.resolve(fileName);
        List<String> reportLines = new ArrayList<>(lines);
        reportLines.add("");
        reportLines.add("All tags without values:");

        if (tagsWithoutValues.isEmpty()) {
            reportLines.add("None");
        } else {
            for (Map.Entry<String, Integer> tag : tagsWithoutValues) {
                double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (tag.getValue() * 100.0) / itemsWithoutEMC;
                reportLines.add("#" + tag.getKey() + " - " + formatPercent(percentOfMissingItems));
            }
        }

        reportLines.add("");
        reportLines.add("All missing recipe ingredients:");

        if (recipeUnlockItems.isEmpty()) {
            reportLines.add("None");
        } else {
            for (Map.Entry<String, EMCValues.RecipeUnlockInfo> item : recipeUnlockItems) {
                reportLines.add(formatRecipeUnlockItem(item, itemsWithoutEMC));
            }
        }

        try {
            Files.createDirectories(reportDir);
            Files.writeString(reportPath, String.join("\n", reportLines) + "\n");
        } catch (IOException e) {
            VanillaEMC.LOGGER.error("Could not write EMC debug report to {}", reportPath, e);
        }

        return reportPath;
    }

    private static Path writeNamespaceReport(
        List<String> lines,
        String namespace,
        List<Map.Entry<String, Integer>> namespacesWithMissingItems,
        HashMap<String, List<String>> missingItemsByNamespace,
        List<Map.Entry<String, Integer>> tagsWithoutValues,
        List<Map.Entry<String, EMCValues.RecipeUnlockInfo>> recipeUnlockItems,
        int itemsWithoutEMC
    ) {
        Path reportDir = Path.of("debug");
        String namespacePart = namespace == null ? "all" : namespace;
        String fileName = "dissolver-namespace-debug-" + namespacePart + "-" + LocalDateTime.now().format(REPORT_DATE_FORMAT);
        Path reportPath = reportDir.resolve(fileName);
        List<String> reportLines = new ArrayList<>(lines);

        reportLines.add("");
        reportLines.add("All namespaces with missing EMC:");
        if (namespacesWithMissingItems.isEmpty()) {
            reportLines.add("None");
        } else {
            for (Map.Entry<String, Integer> itemNamespace : namespacesWithMissingItems) {
                double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (itemNamespace.getValue() * 100.0) / itemsWithoutEMC;
                reportLines.add(itemNamespace.getKey() + " - " + formatPercent(percentOfMissingItems) + " (" + itemNamespace.getValue() + " items)");
            }
        }

        reportLines.add("");
        reportLines.add("All missing items:");
        if (missingItemsByNamespace.isEmpty()) {
            reportLines.add("None");
        } else {
            List<String> itemNamespaces = missingItemsByNamespace
                .keySet()
                .stream()
                .sorted()
                .toList();
            for (String itemNamespace : itemNamespaces) {
                List<String> missingItems = missingItemsByNamespace.get(itemNamespace)
                    .stream()
                    .sorted()
                    .toList();
                reportLines.add(itemNamespace + " (" + missingItems.size() + " items)");
                for (String itemId : missingItems) {
                    reportLines.add("- " + itemId);
                }
            }
        }

        if (namespace != null) {
            reportLines.add("");
            reportLines.add("All tags without values:");
            if (tagsWithoutValues.isEmpty()) {
                reportLines.add("None");
            } else {
                for (Map.Entry<String, Integer> tag : tagsWithoutValues) {
                    double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (tag.getValue() * 100.0) / itemsWithoutEMC;
                    reportLines.add("#" + tag.getKey() + " - " + formatPercent(percentOfMissingItems));
                }
            }

            reportLines.add("");
            reportLines.add("All missing recipe ingredients:");
            if (recipeUnlockItems.isEmpty()) {
                reportLines.add("None");
            } else {
                for (Map.Entry<String, EMCValues.RecipeUnlockInfo> item : recipeUnlockItems) {
                    reportLines.add(formatRecipeUnlockItem(item, itemsWithoutEMC));
                }
            }
        }

        try {
            Files.createDirectories(reportDir);
            Files.writeString(reportPath, String.join("\n", reportLines) + "\n");
        } catch (IOException e) {
            VanillaEMC.LOGGER.error("Could not write EMC namespace debug report to {}", reportPath, e);
        }

        return reportPath;
    }

    private static Path writeRecipeReport(String itemId, List<String> recipeLines) {
        Path reportDir = Path.of("debug");
        String safeItemId = itemId.replace(":", "-");
        String fileName = "dissolver-recipe-debug-" + safeItemId + "-" + LocalDateTime.now().format(REPORT_DATE_FORMAT);
        Path reportPath = reportDir.resolve(fileName);

        try {
            Files.createDirectories(reportDir);
            Files.writeString(reportPath, String.join("\n", recipeLines) + "\n");
        } catch (IOException e) {
            VanillaEMC.LOGGER.error("Could not write EMC recipe debug report to {}", reportPath, e);
        }

        return reportPath;
    }

    private static long countRecipes(List<String> recipeLines) {
        return recipeLines
            .stream()
            .filter(line -> line.startsWith("Recipe: "))
            .count();
    }

    private static String formatRecipeUnlockItem(Map.Entry<String, EMCValues.RecipeUnlockInfo> item, int itemsWithoutEMC) {
        double percentOfMissingItems = itemsWithoutEMC == 0 ? 0 : (item.getValue().count() * 100.0) / itemsWithoutEMC;
        return item.getKey() + " - " + formatPercent(percentOfMissingItems) + " (" + item.getValue().count() +
            " items, " + item.getValue().reason() + ")";
    }

    private static String getNamespace(String itemId) {
        int separator = itemId.indexOf(":");
        return separator < 0 ? "minecraft" : itemId.substring(0, separator);
    }
}
