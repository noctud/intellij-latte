package dev.noctud.latte.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.util.Key;
import com.intellij.psi.tree.IElementType;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.psi.LatteTypes;
import dev.noctud.latte.settings.LatteTagSettings;
import dev.noctud.latte.utils.LatteHtmlUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * External rules for LatteParser.
 */
public class LatteParserUtil extends GeneratedParserUtilBase {

    /**
     * The one walk {@link #hasArrowBeforeTheTagCloses} makes over a tag, kept for the rest of
     * that tag's items. It lives on the builder, so it lasts exactly as long as the parse of one
     * file and two of them never see each other's.
     */
    private static final Key<int[]> LAST_ARROW_IN_TAG = Key.create("latte.parser.lastArrowInTag");

    /**
     * Looks for a classic macro a returns true if it finds the macro a and it is pair or unpaired (based on pair parameter).
     */
    public static boolean checkPairMacro(PsiBuilder builder, int level, Parser parser) {
        if (builder.getTokenType() != LatteTypes.T_MACRO_OPEN_TAG_OPEN) return false;

        PsiBuilder.Marker marker = builder.mark();
        String macroName = getMacroName(builder);

        boolean pair = parser == LatteParser.TRUE_parser_;
        boolean result;

        LatteTagSettings tag = getTag(builder);
        if (tag == null || tag.getType() == LatteTagSettings.Type.AUTO_EMPTY) {
            result = pair == isPair(macroName, builder);
        } else if (macroName.equals("_")) {
            // hard coded rule for macro _ because of dg's poor design decision
            // macro _ is pair only if it has empty arguments, otherwise it is unpaired
            // see https://github.com/nette/nette/blob/v2.1.2/Nette/Latte/Macros/CoreMacros.php#L193
            boolean emptyArgs = true;
            builder.advanceLexer();
            while (emptyArgs && nextTokenIsFast(builder, LatteTypes.T_MACRO_ARGS, LatteTypes.T_MACRO_ARGS_NUMBER, LatteTypes.T_MACRO_ARGS_STRING, LatteTypes.T_MACRO_ARGS_VAR, LatteTypes.T_PHP_METHOD)) {
                emptyArgs = (builder.getTokenText().trim().length() == 0);
                builder.advanceLexer();
            }
            result = (emptyArgs == pair);

            // all other macros which respect rules
        } else {
            result = (tag != null ? (pair ? (LatteTagSettings.Type.PAIR == tag.getType()) : LatteTagSettings.Type.unpairedSet.contains(tag.getType())) : !pair);
        }

        marker.rollbackTo();
        return result;
    }

    public static boolean checkPairHtmlTag(PsiBuilder builder, int level, Parser parser) {
        boolean pair = parser == LatteParser.TRUE_parser_;
        if (builder.getTokenType() != LatteTypes.T_HTML_OPEN_TAG_OPEN) return false;

        PsiBuilder.Marker marker = builder.mark();
        String tagName = getHtmlTagName(builder);

        boolean isVoidTag = LatteHtmlUtil.isVoidTag(tagName);
        boolean result = (!isVoidTag && pair) || (isVoidTag && !pair);

        marker.rollbackTo();
        return result;
    }

    public static boolean checkEmptyMacro(PsiBuilder builder, int level) {
        PsiBuilder.Marker marker = builder.mark();
        boolean result = false;
        while (true) {
            IElementType token = builder.getTokenType();
            if (token == null) {
                break;
            } else if (token == LatteTypes.T_MACRO_TAG_CLOSE) {
                break;
            } else if (token == LatteTypes.T_MACRO_TAG_CLOSE_EMPTY) {
                result = true;
                break;
            }
            builder.advanceLexer();
        }
        marker.rollbackTo();

        return result;
    }

    /**
     * Whether an {@code as} is written in what is left of this tag.
     *
     * phpForeach parses a whole expression before it looks for the {@code as} that
     * would have told it apart from any other expression, so on a tag without one it
     * parses the expression, fails, and rolls it back - and the alternative that was
     * going to match parses the identical text again. The outer rule is a list, so
     * that happened once per item and the cost of one tag grew with the square of the
     * number of items in it: an argument list, a chain of filters or a body of
     * statements cost 4x for every doubling of its length.
     *
     * Looking for the one token first costs a walk over the tag and no marker, and it
     * is the same shape of fix as the one phpArrayItem carries: find out cheaply
     * whether the expensive alternative can match before parsing anything.
     *
     * The walk stops at the end of the tag, see {@link #isTagBoundary}.
     */
    public static boolean hasAsBeforeTheTagCloses(PsiBuilder builder, int level) {
        return hasBeforeTheTagCloses(builder, current -> current.getTokenType() == LatteTypes.T_PHP_AS);
    }

    /**
     * Whether an arrow is written in what is left of this tag.
     *
     * phpKeyArrayItem parses a whole key before it looks for the arrow, the same shape of cost
     * as phpForeach above: without a separator to stop the key, it took the whole rest of the
     * tag at every item and threw it away, so {@code &#123;= $a + $a + ...&#125;} cost 4x per
     * doubling of its length.
     *
     * In the arguments of a link the arrow is lexed as plain {@code T_MACRO_ARGS} text, which
     * is why the rule spells it both ways, so both are looked for. The text is read only for
     * that one token type.
     *
     * Unlike the "as" above, this one is asked once per item of the list rather than once for
     * the tag, so it must not walk. A walk per item over the rest of the tag is quadratic in
     * the width of the tag all over again, only with a cheaper constant: a body of 2048
     * statements cost 691 ms that way against 94 ms without the guard at all.
     *
     * The tag is therefore walked once and what that walk found is kept - where the tag ends,
     * and where the last arrow before it is. Every other item of the same tag is then a
     * comparison, because an arrow is still ahead exactly when the last one of the tag is at
     * or after where we stand.
     */
    public static boolean hasArrowBeforeTheTagCloses(PsiBuilder builder, int level) {
        int offset = builder.getCurrentOffset();
        int[] tag = builder.getUserData(LAST_ARROW_IN_TAG);
        if (tag == null || offset < tag[0] || offset >= tag[1]) {
            tag = scanForTheLastArrow(builder);
            builder.putUserData(LAST_ARROW_IN_TAG, tag);
        }

        return tag[2] >= offset;
    }

    /**
     * What one walk over a tag found: {@code {walked from, tag ends at, last arrow at}}, the last
     * of them -1 when the tag holds no arrow.
     *
     * The answer is good for every offset from where the walk started up to where the tag ends,
     * because the end of a tag is the first boundary token at or after the walk, so every offset
     * in between reaches that same one. Anything outside that range is another tag, or a place
     * the parser has backtracked to, and is walked again.
     */
    private static int[] scanForTheLastArrow(PsiBuilder builder) {
        int from = builder.getCurrentOffset();
        int lastArrow = -1;

        PsiBuilder.Marker marker = builder.mark();
        while (!isTagBoundary(builder.getTokenType())) {
            if (isArrow(builder)) {
                lastArrow = builder.getCurrentOffset();
            }
            builder.advanceLexer();
        }
        int tagEnd = builder.getCurrentOffset();
        marker.rollbackTo();

        return new int[]{from, tagEnd, lastArrow};
    }

    private static boolean isArrow(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        return type == LatteTypes.T_PHP_DOUBLE_ARROW
            || (type == LatteTypes.T_MACRO_ARGS && "=>".equals(builder.getTokenText()));
    }

    private static boolean hasBeforeTheTagCloses(PsiBuilder builder, Predicate<PsiBuilder> wanted) {
        PsiBuilder.Marker marker = builder.mark();
        boolean result = false;
        while (!isTagBoundary(builder.getTokenType())) {
            if (wanted.test(builder)) {
                result = true;
                break;
            }
            builder.advanceLexer();
        }
        marker.rollbackTo();

        return result;
    }

    /**
     * Where a walk over what is left of a tag has to stop: the end of the tag, or of the n:
     * attribute or the element the tag sits in. A walk that went past it would answer about
     * somebody else's tokens, and one left to run to the end of the file would replace one
     * quadratic cost with another.
     */
    private static boolean isTagBoundary(IElementType type) {
        return type == null
            || type == LatteTypes.T_MACRO_TAG_CLOSE
            || type == LatteTypes.T_MACRO_TAG_CLOSE_EMPTY
            || type == LatteTypes.T_HTML_TAG_ATTR_SQ
            || type == LatteTypes.T_HTML_TAG_ATTR_DQ
            || type == LatteTypes.T_HTML_TAG_ATTR_CURLY_RIGHT
            || type == LatteTypes.T_HTML_TAG_CLOSE
            || type == LatteTypes.T_HTML_OPEN_TAG_CLOSE;
    }

    /**
     * Whether the bracket at the current position has its own closing one still inside this tag.
     *
     * phpArray parses a whole array before it can find out that the brackets never close, and it
     * recurses back into itself, so a run of unclosed brackets cost the square of its length: 512
     * of them took 623 ms and an editor freezes on far less. Looking for the closing bracket first
     * costs a walk over the tag and no marker.
     *
     * A pin on the rule would be cheaper still and is the usual answer to an alternative that
     * parses a subtree and backs out - but it was tried and it turned two correct templates red,
     * because a bracket that opens no array then reports an error instead of letting the next
     * alternative have it. In this plugin a false report is the one thing that must not happen, so
     * the more expensive guard is the right one.
     *
     * Anything that is not a bracket is let through: the rule's other form is {@code array(...)},
     * and deciding for it is not this guard's business.
     */
    public static boolean hasMatchingBracketBeforeTagCloses(PsiBuilder builder, int level) {
        if (builder.getTokenType() != LatteTypes.T_PHP_LEFT_BRACKET) {
            return true;
        }
        PsiBuilder.Marker marker = builder.mark();
        int depth = 0;
        boolean closed = false;
        while (true) {
            IElementType type = builder.getTokenType();
            if (isTagBoundary(type)) {
                break;
            }
            if (type == LatteTypes.T_PHP_LEFT_BRACKET) {
                depth++;
            } else if (type == LatteTypes.T_PHP_RIGHT_BRACKET) {
                depth--;
                if (depth == 0) {
                    closed = true;
                    break;
                }
            }
            builder.advanceLexer();
        }
        marker.rollbackTo();

        return closed;
    }

    @NotNull
    private static String getMacroName(PsiBuilder builder) {
        String macroName;

        consumeTokenFast(builder, LatteTypes.T_MACRO_OPEN_TAG_OPEN);
        consumeTokenFast(builder, LatteTypes.T_MACRO_CLOSE_TAG_OPEN);
        consumeTokenFast(builder, LatteTypes.T_MACRO_NOESCAPE);
        if (nextTokenIsFast(builder, LatteTypes.T_MACRO_NAME, LatteTypes.T_MACRO_SHORTNAME)) {
            macroName = builder.getTokenText();
            assert macroName != null;

        } else {
            macroName = "=";
        }
        return macroName;
    }

    @NotNull
    private static String getHtmlTagName(PsiBuilder builder) {
        String macroName = "?";

        consumeTokenFast(builder, LatteTypes.T_HTML_OPEN_TAG_OPEN);
        if (nextTokenIsFast(builder, LatteTypes.T_TEXT)) {
            macroName = builder.getTokenText();
            if (macroName != null && macroName.length() > 0) {
                macroName = macroName.split(" ")[0];

            } else {
                macroName = "?";
            }
        }
        return macroName;
    }

    private static boolean isPair(String macroName, PsiBuilder builder) {
        builder.advanceLexer();
        IElementType type = builder.getTokenType();
        while (type != null) {
            if (type == LatteTypes.T_MACRO_TAG_CLOSE_EMPTY) {
                return true;
            } else if (type == LatteTypes.T_MACRO_TAG_CLOSE) {
                break;
            }
            builder.advanceLexer();
            type = builder.getTokenType();
        }
        int depth = 0;
        while (type != null) {
            if (nextTokenIsFast(builder, LatteTypes.T_MACRO_CLOSE_TAG_OPEN, LatteTypes.T_MACRO_OPEN_TAG_OPEN) && getMacroName(builder).equals(macroName)) {
                if (type == LatteTypes.T_MACRO_CLOSE_TAG_OPEN) {
                    if (depth == 0) {
                        return true;
                    }
                    depth--;
                } else {
                    depth++;
                }
            } else if (type == LatteTypes.T_HTML_TAG_NATTR_NAME && ("n:" + macroName).equals(builder.getTokenText())) {
                builder.advanceLexer();
                type = builder.getTokenType();
                continue;
            }
            builder.advanceLexer();
            type = builder.getTokenType();
        }
        return false;
    }

    private static boolean isEmptyPair(PsiBuilder builder) {
        IElementType type = builder.getTokenType();
        while (type != null) {
            if (type == LatteTypes.T_MACRO_TAG_CLOSE_EMPTY) {
                return true;
            } else if (type == LatteTypes.T_MACRO_TAG_CLOSE) {
                return false;
            }
            builder.advanceLexer();
            type = builder.getTokenType();
        }
        return false;
    }

    public static boolean isNamespace(PsiBuilder builder, int level) {
        PsiBuilder.Marker marker = builder.mark();

        boolean result = false;

        IElementType type = builder.getTokenType();
        IElementType nextToken = builder.lookAhead(1);
        if (type == LatteTypes.T_PHP_NAMESPACE_REFERENCE && nextToken == LatteTypes.T_PHP_NAMESPACE_RESOLUTION) {
            result = true;
        }

        marker.rollbackTo();

        return result;
    }

    private static LatteTagSettings getTag(PsiBuilder builder) {
        return LatteConfiguration.getInstance(builder.getProject()).getTag(getMacroName(builder));
    }

}
