package dev.noctud.latte.completion.matchers;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public final class StartsWithMatcher extends PrefixMatcher {
    public StartsWithMatcher(@NotNull String prefix) {
        super(prefix);
    }

    @Override
    public boolean prefixMatches(@NotNull String name) {
        return StringUtil.startsWithIgnoreCase(name, getPrefix()); // or name.startsWith(getPrefix())
    }

    @Override
    public @NotNull PrefixMatcher cloneWithPrefix(@NotNull String newPrefix) {
        return new StartsWithMatcher(newPrefix);
    }
}
