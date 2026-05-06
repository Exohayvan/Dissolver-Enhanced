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

        List<Map.Entry<String, Integer>> topTags = missingTagCounts
            .entrySet()
            .stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
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

        Path reportPath = writeReport(lines);
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
            "Config Override: " + (configOverride ? "Yes" : "No"),
            "Has Components: " + (!stack.getComponentChanges().isEmpty() ? "Yes" : "No"),
            "Learnable: " + (emc > 0 ? "Yes" : "No")
        );

        VanillaEMC.LOGGER.info(debugText);
        ModCommands.feedback(context, debugText);
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

    private static Path writeReport(List<String> lines) {
        Path reportDir = Path.of("debug");
        String fileName = "dissolver-debug-report-" + LocalDateTime.now().format(REPORT_DATE_FORMAT);
        Path reportPath = reportDir.resolve(fileName);

        try {
            Files.createDirectories(reportDir);
            Files.writeString(reportPath, String.join("\n", lines) + "\n");
        } catch (IOException e) {
            VanillaEMC.LOGGER.error("Could not write EMC debug report to {}", reportPath, e);
        }

        return reportPath;
    }
}
