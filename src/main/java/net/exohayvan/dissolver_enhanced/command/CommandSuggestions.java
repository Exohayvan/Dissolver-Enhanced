package net.exohayvan.dissolver_enhanced.command;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

final class CommandSuggestions {
    private CommandSuggestions() {
    }

    static CompletableFuture<Suggestions> suggest(Iterable<String> values, SuggestionsBuilder builder) {
        for (String value : values) {
            builder.suggest(value);
        }
        return builder.buildFuture();
    }
}
