package dev.noctud.latte.lexer;

import com.intellij.psi.tree.IElementType;
import static dev.noctud.latte.psi.LatteTypes.*;

%%

%class LatteMacroLexer
%extends LatteBaseFlexLexer
%function advance
%type IElementType
%unicode
%ignorecase

%state NAME_ANY
%state NAME_NOT_Q
%state NAME_SHORT
%state ARGS

NAME_FULL = [a-zA-Z][a-zA-Z0-9_]* ([.:][a-zA-Z0-9_]+)*

%{
	private boolean doubleBraceMode = false;
%}

%%
<YYINITIAL> {
	"{{/" {
		doubleBraceMode = true;
		yybegin(NAME_NOT_Q);
		return T_MACRO_CLOSE_TAG_OPEN;
	}

	"{{" {
		doubleBraceMode = true;
		yybegin(NAME_ANY);
		return T_MACRO_OPEN_TAG_OPEN;
	}

	"{/" {
		doubleBraceMode = false;
		yybegin(NAME_NOT_Q);
		return T_MACRO_CLOSE_TAG_OPEN;
	}

	"{" {
		doubleBraceMode = false;
		yybegin(NAME_ANY);
		return T_MACRO_OPEN_TAG_OPEN;
	}
}

<NAME_ANY> {
	"?" {
		yybegin(ARGS);
		return T_MACRO_NAME;
	}
}

<NAME_ANY, NAME_NOT_Q> {
	"!" {
		yybegin(NAME_SHORT);
		return T_MACRO_NOESCAPE;
	}

	{NAME_FULL} {
		yybegin(ARGS);
		return T_MACRO_NAME;
	}

	{NAME_FULL} ("::" | "(" | "\\") {
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}
}

<NAME_ANY, NAME_NOT_Q, NAME_SHORT> {
	[=~#%\^&_] {
		yybegin(ARGS);
		return T_MACRO_SHORTNAME;
	}
}

<NAME_ANY, NAME_NOT_Q, NAME_SHORT, ARGS> {
	// A block comment is taken whole, and this is the only reason why. Read one character at a
	// time, a comment ending a tag falls apart: the */ before the closing brace leaves a / and a }
	// side by side, which is how a tag says it closes itself - so {var $a = 1 /* note */} ended in
	// the middle of its own comment. Latte compiles that template, so nothing here may report it.
	// Longest match wins in JFlex, so this beats both the single character below and "/}".
	// An opening without an ending still matches nothing, which leaves the tag as it is today
	// rather than reading to the end of the file looking for one.
	"/*" ~"*/" {
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}

	"}}" {
		if (doubleBraceMode) {
			yybegin(ARGS);
			return T_MACRO_TAG_CLOSE;
		}
		// In single-brace mode, first } is content (matching "}" / [^] behavior)
		yypushback(1);
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}

	"}" {
		yybegin(ARGS);
		return T_MACRO_TAG_CLOSE;
	}

	"}" / [^] {
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}

	"/}}" {
		if (doubleBraceMode) {
			yybegin(ARGS);
			return T_MACRO_TAG_CLOSE_EMPTY;
		}
		// In single-brace mode, /} is content (matching "/}" / [^] behavior)
		yypushback(1);
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}

	"/}" {
		yybegin(ARGS);
		return T_MACRO_TAG_CLOSE_EMPTY;
	}

	"/}" / [^] {
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}

	[^] {
		yybegin(ARGS);
		return T_MACRO_CONTENT;
	}
}
