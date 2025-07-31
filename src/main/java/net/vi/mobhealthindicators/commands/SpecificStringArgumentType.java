package net.vi.mobhealthindicators.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SpecificStringArgumentType implements ArgumentType<String> {

    private final Supplier<Set<String>> suggestions;

    SpecificStringArgumentType(Supplier<Set<String>> suggestions) {
        this.suggestions = suggestions;
    }

    public static SpecificStringArgumentType specificString(Supplier<Set<String>> suggestions) {
        return new SpecificStringArgumentType(suggestions);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String text = reader.getString().substring(reader.getCursor());
        if(suggestions.get().contains(text)) {
            reader.setCursor(reader.getTotalLength());
            return text;
        } else {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        for(String suggestion : suggestions.get()) {
            builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }
}
