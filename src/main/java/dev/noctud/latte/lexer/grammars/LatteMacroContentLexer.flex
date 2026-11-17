package dev.noctud.latte.lexer;

import com.intellij.psi.tree.IElementType;
import static dev.noctud.latte.psi.LatteTypes.*;

%%

%class LatteMacroContentLexer
%extends LatteBaseFlexLexer
%function advance
%type IElementType
%unicode
%ignorecase


WHITE_SPACE=[ \t\r\n]+
SYMBOL = [_\p{L}][_0-9\p{L}]*(-[_0-9\p{L}]+)*
FUNCTION_CALL=[a-zA-Z_][a-zA-Z0-9_]* "("
CLASS_NAME=\\?[a-zA-Z_][a-zA-Z0-9_]*\\[a-zA-Z_][a-zA-Z0-9_\\]* | \\[a-zA-Z_][a-zA-Z0-9_]*
CONTENT_TYPE=[a-zA-Z\-][a-zA-Z0-9\-]*\/[a-zA-Z\-][a-zA-Z0-9\-\.]*
FILE_IMPORT=[\w\-.@()#$%\^&*()!\/]+ ".latte"
SIGNAL=[a-zA-Z\-\:]+ "!"

%%


<YYINITIAL> {

	// Inline Latte expression: consume up to and including the first '}' on the same line as part of content
	"{" [^}\r\n]* "}" {
        return T_PHP_CONTENT;
    }

	// General PHP-ish starters: produce content but do not consume macro closer '}' or newline
	// Allows one level of balanced braces (e.g. closures: function() { ... })
	// "match (" is kept in one piece so that the PHP lexer sees the bracket that makes it a keyword
	({CLASS_NAME} | "$" | {FUNCTION_CALL} | "match" [ \t]+ "(" | "\"" | "'" | "(" | "[" | "|") ([^{}\r\n] | "{" [^}\r\n]* "}")* {
        return T_PHP_CONTENT;
    }

    {CONTENT_TYPE} {
        return T_PHP_CONTENT;
    }

	([0-9]+ | {SYMBOL} | {CLASS_NAME}) {
		return T_PHP_CONTENT;
	}

    {WHITE_SPACE} {
        return T_WHITESPACE;
    }

    {FILE_IMPORT} {
        return T_FILE_PATH;
    }

    {SIGNAL} {
        return T_LINK;
    }

    // A brace the rules above could not pair on one line, such as those of a match or a closure
    // whose body spans several lines. It is PHP: the macro lexer never puts the brace closing the
    // tag into the content, and the n: attribute states keep their delimiters out of it as well.
    "{" | "}" {
        return T_PHP_CONTENT;
    }

	[^] {
		return T_MACRO_ARGS;
	}
}
