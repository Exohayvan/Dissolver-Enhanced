package net.exohayvan.dissolver_enhanced.command;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.minecraft.commands.CommandSourceStack;

public class ItemSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
	@Override
	public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
		Set<String> itemIds = EMCValues.getList();

		for (String itemId : itemIds) {
			builder.suggest(itemId);
		}

		return builder.buildFuture();
	}
}
