package net.vi.mobhealthindicator.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class SpecificStringArgumentType implements ArgumentType<String> {

    private final Supplier<String[]> suggestions;
    private final StringArgumentType stringArgumentType;

    private SpecificStringArgumentType(Supplier<String[]> suggestions) {
        stringArgumentType = StringArgumentType.greedyString();
        this.suggestions = suggestions;
    }

    public static SpecificStringArgumentType specificString(Supplier<String[]> suggestions) {
        return new SpecificStringArgumentType(suggestions);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return stringArgumentType.parse(reader);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        for(String suggestion : suggestions.get()) {
            builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }
}
