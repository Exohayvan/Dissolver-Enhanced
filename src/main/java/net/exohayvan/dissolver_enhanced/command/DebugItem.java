package net.exohayvan.dissolver_enhanced.command;

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
import net.exohayvan.dissolver_enhanced.DissolverEnhanced;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;

public class DebugItem {
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static int summary(CommandContext<ServerCommandSource> context, String command) {
        List<Item> items = Registries.ITEM
            .stream()
            .filter(item -> !item.getDefaultStack().isEmpty())
            .toList();

        int totalItems = items.size();
        DebugScan scan = scanItems(items);
        int itemsWithEMC = scan.itemsWithEmc();
        int itemsWithoutEMC = scan.itemsWithoutEmc();
        HashMap<String, Integer> missingTagCounts = scan.missingTagCounts();

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
        DissolverEnhanced.LOGGER.info(debugText);
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
        DebugScan scan = scanItems(items);
        int itemsWithEMC = scan.itemsWithEmc();
        int itemsWithoutEMC = scan.itemsWithoutEmc();
        HashMap<String, Integer> missingNamespaceCounts = scan.missingNamespaceCounts();
        HashMap<String, List<String>> missingItemsByNamespace = scan.missingItemsByNamespace();
        HashMap<String, Integer> missingTagCounts = scan.missingTagCounts();

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
        DissolverEnhanced.LOGGER.info(debugText);
        ModCommands.feedback(context, debugText);
        return 1;
    }

    private static DebugScan scanItems(List<Item> items) {
        int itemsWithEmc = 0;
        int itemsWithoutEmc = 0;
        HashMap<String, Integer> missingNamespaceCounts = new HashMap<>();
        HashMap<String, List<String>> missingItemsByNamespace = new HashMap<>();
        HashMap<String, Integer> missingTagCounts = new HashMap<>();

        for (Item item : items) {
            String itemId = item.toString();
            if (EMCValues.get(itemId) > 0) {
                itemsWithEmc++;
                continue;
            }

            itemsWithoutEmc++;
            String namespace = getNamespace(itemId);
            missingNamespaceCounts.merge(namespace, 1, Integer::sum);
            missingItemsByNamespace.computeIfAbsent(namespace, ignored -> new ArrayList<>()).add(itemId);
            item.getDefaultStack()
                .streamTags()
                .map(TagKey::id)
                .map(Identifier::toString)
                .filter(tagId -> !EMCValues.EMC_TAG_VALUES.containsKey(tagId))
                .forEach(tagId -> missingTagCounts.merge(tagId, 1, Integer::sum));
        }

        return new DebugScan(
            itemsWithEmc,
            itemsWithoutEmc,
            missingNamespaceCounts,
            missingItemsByNamespace,
            missingTagCounts
        );
    }

    private record DebugScan(
        int itemsWithEmc,
        int itemsWithoutEmc,
        HashMap<String, Integer> missingNamespaceCounts,
        HashMap<String, List<String>> missingItemsByNamespace,
        HashMap<String, Integer> missingTagCounts
    ) {
    }

    private static ItemStack heldStack(
        CommandContext<ServerCommandSource> context,
        String emptyMessage
    ) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            ModCommands.feedback(context, "This command must be run by a player.");
            return null;
        }

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty()) {
            ModCommands.feedback(context, emptyMessage);
            return null;
        }
        return stack;
    }

    public static int item(CommandContext<ServerCommandSource> context, String command) {
        ItemStack stack = heldStack(context, "Hold an item to debug it.");
        if (stack == null) return 0;

        String itemId = stack.getItem().toString();
        String emcKey = EMCKey.fromStack(stack);
        int emc = EMCValues.get(emcKey);
        String emcText = emc > 0 ? String.valueOf(emc) : "None";
        String stackEmcText = emc > 0 ? String.valueOf(emc * stack.getCount()) : "None";
        boolean configOverride = EMCValues.isConfigOverridden(emcKey) || EMCValues.isConfigOverridden(itemId);

        List<TagKey<Item>> tags = stack.streamTags().toList();
        List<String> lines = new ArrayList<>(List.of(
            "Name: " + stack.getName().getString(),
            "ID: " + itemId,
            "EMC Key: " + emcKey,
            "Count: " + stack.getCount(),
            "Minecraft Tags: " + formatTags(tags, "minecraft"),
            "Common Tags: " + formatTags(tags, "c"),
            "Other Tags: " + formatOtherTags(tags),
            "EMC: " + emcText,
            "Stack EMC: " + stackEmcText,
            "EMC Source: " + EMCValues.getSource(emcKey),
            "EMC Source Detail: " + EMCValues.getSourceDetail(emcKey),
            "Config Override: " + (configOverride ? "Yes" : "No"),
            "Has Components: " + (!stack.getComponentChanges().isEmpty() ? "Yes" : "No"),
            "Learnable: " + (emc > 0 ? "Yes" : "No")
        ));
        List<String> componentLines = EMCKey.describe(stack);
        if (!componentLines.isEmpty()) {
            lines.add("Component Scan:");
            lines.addAll(componentLines);
        }

        String debugText = String.join("\n", lines);

        DissolverEnhanced.LOGGER.info(debugText);
        ModCommands.feedback(context, debugText);
        return 1;
    }

    public static int recipe(CommandContext<ServerCommandSource> context, String command) {
        ItemStack stack = heldStack(context, "Hold an item to debug its recipes.");
        if (stack == null) return 0;

        String itemId = stack.getItem().toString();
        List<String> recipeLines = EMCValues.getRecipeDebugLines(itemId);
        Path reportPath = writeRecipeReport(itemId, recipeLines);

        String chatText = String.join("\n",
            "Recipe Debug: " + itemId,
            "Recipes found: " + countRecipes(recipeLines),
            "Report: " + reportPath
        );
        String logText = "Recipe Debug: " + itemId + "\n" + String.join("\n", recipeLines);

        DissolverEnhanced.LOGGER.info(logText);
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
        appendMissingDetails(reportLines, tagsWithoutValues, recipeUnlockItems, itemsWithoutEMC);

        try {
            Files.createDirectories(reportDir);
            Files.writeString(reportPath, String.join("\n", reportLines) + "\n");
        } catch (IOException e) {
            DissolverEnhanced.LOGGER.error("Could not write EMC debug report to {}", reportPath, e);
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
            appendMissingDetails(reportLines, tagsWithoutValues, recipeUnlockItems, itemsWithoutEMC);
        }

        try {
            Files.createDirectories(reportDir);
            Files.writeString(reportPath, String.join("\n", reportLines) + "\n");
        } catch (IOException e) {
            DissolverEnhanced.LOGGER.error("Could not write EMC namespace debug report to {}", reportPath, e);
        }

        return reportPath;
    }

    private static void appendMissingDetails(
        List<String> reportLines,
        List<Map.Entry<String, Integer>> tagsWithoutValues,
        List<Map.Entry<String, EMCValues.RecipeUnlockInfo>> recipeUnlockItems,
        int itemsWithoutEmc
    ) {
        reportLines.add("All tags without values:");
        if (tagsWithoutValues.isEmpty()) {
            reportLines.add("None");
        } else {
            for (Map.Entry<String, Integer> tag : tagsWithoutValues) {
                double percent = itemsWithoutEmc == 0 ? 0 : (tag.getValue() * 100.0) / itemsWithoutEmc;
                reportLines.add("#" + tag.getKey() + " - " + formatPercent(percent));
            }
        }

        reportLines.add("");
        reportLines.add("All missing recipe ingredients:");
        if (recipeUnlockItems.isEmpty()) {
            reportLines.add("None");
        } else {
            for (Map.Entry<String, EMCValues.RecipeUnlockInfo> item : recipeUnlockItems) {
                reportLines.add(formatRecipeUnlockItem(item, itemsWithoutEmc));
            }
        }
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
            DissolverEnhanced.LOGGER.error("Could not write EMC recipe debug report to {}", reportPath, e);
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
