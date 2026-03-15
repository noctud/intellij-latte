package dev.noctud.latte.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.lexer.LookAheadLexer;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.noctud.latte.utils.LatteTypesUtil;
import dev.noctud.latte.psi.LatteTypes;

/**
 * Lexer used for syntax highlighting
 * <p>
 * It reuses the simple lexer, changing types of some tokens
 */
public class LatteHighlightingLexer extends LookAheadLexer {
    private static final TokenSet WHITESPACES = LatteTypesUtil.whitespaceTokens;

    private boolean lastHashTag = false;
    private IElementType beforeLastToken = null;
    private IElementType beforeBeforeLastToken = null;
    private IElementType lastNoWhitespaceToken = null;
    private IElementType lastToken = null;
    private Lexer lexer;

    public LatteHighlightingLexer(Lexer baseLexer) {
        super(baseLexer, 1);
        lexer = baseLexer;
    }

    @Override
    protected void addToken(int endOffset, IElementType type) {
        if (lastToken == LatteTypes.T_PHP_NAMESPACE_RESOLUTION && (type == LatteTypes.T_PHP_IDENTIFIER)) {
            type = LatteTypes.T_PHP_NAMESPACE_REFERENCE;
        } else if (type == LatteTypes.T_PHP_IDENTIFIER && lastHashTag) {
            type = LatteTypes.T_BLOCK_NAME;
        }

        super.addToken(endOffset, type);
        beforeBeforeLastToken = WHITESPACES.contains(beforeLastToken) ? beforeBeforeLastToken : beforeLastToken;
        beforeLastToken = WHITESPACES.contains(lastToken) ? beforeLastToken : lastToken;
        lastNoWhitespaceToken = WHITESPACES.contains(type) ? lastNoWhitespaceToken : type;
        lastToken = type;
        if (!WHITESPACES.contains(type)) {
            lastHashTag = type == LatteTypes.T_MACRO_ARGS && LatteLookAheadLexer.isCharacterAtCurrentPosition(lexer, '#');
        }
    }

    @Override
    protected void lookAhead(Lexer baseLexer) {
        IElementType currentToken = baseLexer.getTokenType();

        if (currentToken == LatteTypes.T_PHP_IDENTIFIER) {
            currentToken = getNextTokenType(baseLexer);

            if (currentToken == LatteTypes.T_PHP_LEFT_NORMAL_BRACE) {
                replaceCachedType(0, LatteTypes.T_PHP_METHOD);
            } else {
                super.lookAhead(baseLexer);
            }

        } else if (currentToken == LatteTypes.T_PHP_NAMESPACE_REFERENCE && lastToken == LatteTypes.T_PHP_NAMESPACE_RESOLUTION && beforeLastToken != LatteTypes.T_PHP_NEW) {
            currentToken = getNextTokenType(baseLexer);

            if (currentToken == LatteTypes.T_PHP_LEFT_NORMAL_BRACE) {
                replaceCachedType(0, LatteTypes.T_PHP_METHOD);
            } else {
                super.lookAhead(baseLexer);
            }

        } else if (currentToken == LatteTypes.T_MACRO_ARGS && LatteLookAheadLexer.isCharacterAtCurrentPosition(baseLexer, '#')) {
            currentToken = getNextTokenType(baseLexer);

            if (currentToken == LatteTypes.T_PHP_IDENTIFIER) {
                replaceCachedType(0, LatteTypes.T_BLOCK_NAME);
            }

        } else {
            super.lookAhead(baseLexer);
        }
    }

    private IElementType getNextTokenType(Lexer baseLexer) {
        advanceLexer(baseLexer);
        IElementType currentToken = baseLexer.getTokenType();
        if (WHITESPACES.contains(currentToken)) {
            advanceLexer(baseLexer);
            currentToken = baseLexer.getTokenType();
        }
        return currentToken;
    }
}
