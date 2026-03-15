package dev.noctud.latte.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LookAheadLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.noctud.latte.psi.LatteTypes;
import dev.noctud.latte.utils.LatteTagsUtil;
import dev.noctud.latte.utils.LatteTypesUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lexer used for syntax highlighting
 * <p>
 * It reuses the simple lexer, changing types of some tokens
 */
public class LatteLookAheadLexer extends LookAheadLexer {
    private static final TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE, LatteTypes.T_WHITESPACE);
    private static final TokenSet TAG_TAGS = TokenSet.create(LatteTypes.T_HTML_TAG_ATTR_EQUAL_SIGN, LatteTypes.T_HTML_TAG_ATTR_DQ);

    private static final String IDENTIFIER_FILES = "files";
    private static final String IDENTIFIER_LINKS = "links";
    private static final String IDENTIFIER_TYPES = "types";
    private static final String IDENTIFIER_CONTROLS = "controls";

    private final Map<String, Boolean> lastValid = new HashMap<>();
    private final Map<String, Boolean> replaceAs = new HashMap<>();

    final private Lexer lexer;

    public LatteLookAheadLexer(Lexer baseLexer) {
        super(baseLexer, 1);
        lexer = baseLexer;
    }

    @Override
    protected void addToken(int endOffset, IElementType type) {
        boolean wasControl = false;
        if ((type == LatteTypes.T_PHP_IDENTIFIER || type == LatteTypes.T_PHP_ADDITIVE_OPERATOR || type == LatteTypes.T_CONTROL || type == LatteTypes.T_PHP_KEYWORD || (type == LatteTypes.T_MACRO_ARGS && isCharacterAtCurrentPosition(lexer, ':', '-'))) && needReplaceAsMacro(IDENTIFIER_CONTROLS)) {
            type = LatteTypes.T_CONTROL;
            wasControl = true;
        }

        boolean wasLink = false;
        if ((type == LatteTypes.T_PHP_IDENTIFIER || type == LatteTypes.T_CONTROL || type == LatteTypes.T_PHP_KEYWORD || (type == LatteTypes.T_MACRO_ARGS && isCharacterAtCurrentPosition(lexer, '#', ':', '/'))) && needReplaceAsMacro(IDENTIFIER_LINKS)) {
            type = LatteTypes.T_LINK;
            wasLink = true;
        }

        boolean wasTypeDefinition = false;
        boolean isMacroSeparator = type == LatteTypes.T_PHP_MACRO_SEPARATOR;
        boolean isMacroFilters = type == LatteTypes.T_MACRO_FILTERS;
        if ((LatteTypesUtil.phpTypeTokens.contains(type) || isMacroSeparator || isMacroFilters) && needReplaceAsMacro(IDENTIFIER_TYPES)) {
            if (isMacroSeparator) {
                type = LatteTypes.T_PHP_OR_INCLUSIVE;
            } else if (isMacroFilters) {
                type = LatteTypes.T_PHP_IDENTIFIER;
            }
            wasTypeDefinition = true;
        }

        boolean wasFilePath = false;
        if ((type == LatteTypes.T_PHP_CONTENT_TYPE || type == LatteTypes.T_PHP_MULTIPLICATIVE_OPERATORS || type == LatteTypes.T_MACRO_ARGS || type == LatteTypes.T_PHP_CONTENT || type == LatteTypes.T_PHP_IDENTIFIER || type == LatteTypes.T_PHP_KEYWORD) && needReplaceAsMacro(IDENTIFIER_FILES)) {
            type = LatteTypes.T_FILE_PATH;
            wasFilePath = true;
        }

        super.addToken(endOffset, type);
        if (!TAG_TAGS.contains(type)) {
            checkMacroType(IDENTIFIER_FILES, type, LatteTagsUtil.FILE_TAGS_LIST, wasFilePath);
            checkMacroType(IDENTIFIER_LINKS, type, LatteTagsUtil.LINK_TAGS_LIST, wasLink);
            checkMacroType(IDENTIFIER_CONTROLS, type, LatteTagsUtil.CONTROL_TAGS_LIST, wasControl);
            checkMacroType(IDENTIFIER_TYPES, type, LatteTagsUtil.TYPE_TAGS_LIST, wasTypeDefinition);
        }
    }

    private boolean needReplaceAsMacro(@NotNull String identifier) {
        return replaceAs.getOrDefault(identifier, false);
    }

    private void checkMacroType(@NotNull String identifier, IElementType type, @NotNull List<String> types, boolean currentValid) {
        boolean current = (type == LatteTypes.T_MACRO_NAME || type == LatteTypes.T_HTML_TAG_NATTR_NAME) && isMacroTypeMacro(lexer, types);

        // Special-case: treat n:name as a control only inside <form> tags
        if (!current && identifier.equals(IDENTIFIER_CONTROLS) && type == LatteTypes.T_HTML_TAG_NATTR_NAME) {
            if (isCurrentTokenEquals(lexer, "n:name") && isInsideCurrentHtmlOpenTagNamed(lexer, "form")) {
                current = true;
            }
        }

        replaceAs.put(identifier, (currentValid && !WHITESPACES.contains(type))
            || (!currentValid && lastValid.containsKey(identifier) && lastValid.getOrDefault(identifier, false) && WHITESPACES.contains(type))
            || current);
        lastValid.put(identifier, current);
    }

    private static boolean isCurrentTokenEquals(@NotNull Lexer baseLexer, @NotNull String text) {
        CharSequence seq = baseLexer.getBufferSequence();
        int start = baseLexer.getTokenStart();
        int end = baseLexer.getTokenEnd();
        if (start < 0 || end > seq.length() || start >= end) {
            return false;
        }
        CharSequence token = seq.subSequence(start, end);
        return text.contentEquals(token);
    }

    private static boolean isInsideCurrentHtmlOpenTagNamed(@NotNull Lexer baseLexer, @NotNull String tagName) {
        CharSequence seq = baseLexer.getBufferSequence();
        int pos = baseLexer.getTokenStart();
        // Walk backwards until we hit '<' (start of tag) or '>' (start of previous tag end)
        for (int i = pos - 1; i >= 0; i--) {
            char ch = seq.charAt(i);
            if (ch == '>') {
                // We passed the beginning of this tag; not inside an open tag context
                break;
            }
            if (ch == '<') {
                int j = i + 1;
                if (j < seq.length() && seq.charAt(j) == '/') {
                    // It's a close tag; not relevant for attribute
                    return false;
                }
                // Read tag name
                StringBuilder name = new StringBuilder();
                while (j < seq.length()) {
                    char c = seq.charAt(j);
                    if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':') {
                        name.append(c);
                        j++;
                    } else {
                        break;
                    }
                }
                return tagName.equalsIgnoreCase(name.toString());
            }
        }
        return false;
    }

    public static boolean isCharacterAtCurrentPosition(Lexer baseLexer, char... characters) {
        char current = baseLexer.getBufferSequence().charAt(baseLexer.getCurrentPosition().getOffset());
        for (char character : characters) {
            if (current == character) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMacroTypeMacro(Lexer baseLexer, @NotNull List<String> types) {
        CharSequence tagName = baseLexer.getBufferSequence().subSequence(baseLexer.getTokenStart(), baseLexer.getTokenEnd());
        if (tagName.length() == 0) {
            return false;
        }

        return types.contains(tagName.toString());
    }
}
