package me.hydos.legacybrigadier.extras;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface CommandSourceExtra {

    static CompletableFuture<Suggestions> suggestMatching(Iterable<String> iterable, SuggestionsBuilder suggestionsBuilder) {
        String string = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
        Iterator var3 = iterable.iterator();

        while(var3.hasNext()) {
            String string2 = (String)var3.next();
            if (string2.toLowerCase(Locale.ROOT).startsWith(string)) {
                suggestionsBuilder.suggest(string2);
            }
        }

        return suggestionsBuilder.buildFuture();
    }

}
