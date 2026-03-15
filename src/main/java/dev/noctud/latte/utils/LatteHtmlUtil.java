package dev.noctud.latte.utils;

import com.intellij.psi.tree.TokenSet;
import dev.noctud.latte.psi.LatteTypes;

import java.util.Arrays;

public class LatteHtmlUtil {
    public static TokenSet HTML_TOKENS = TokenSet.create(LatteTypes.T_TEXT, LatteTypes.T_HTML_CLOSE_TAG_OPEN, LatteTypes.T_HTML_OPEN_TAG_OPEN, LatteTypes.T_HTML_OPEN_TAG_CLOSE, LatteTypes.T_HTML_TAG_CLOSE);

    final private static String[] voidTags = new String[]{
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr", "!doctype",
        "style", "script" // style and script are here only because LatteTopLexer.flex implementation
    };

    public static boolean isVoidTag(String tagName) {
        return Arrays.asList(voidTags).contains(tagName.toLowerCase());
    }
}
