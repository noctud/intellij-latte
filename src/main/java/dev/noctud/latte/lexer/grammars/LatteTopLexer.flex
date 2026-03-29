package dev.noctud.latte.lexer;

import com.intellij.psi.tree.IElementType;
import static dev.noctud.latte.psi.LatteTypes.*;

%%

%class LatteTopLexer
%extends LatteBaseFlexLexer
%function advance
%type IElementType
%unicode
%ignorecase

%state HTML_TEXT
%state HTML_OPEN_TAG_OPEN
%state HTML_CLOSE_TAG_OPEN
%state HTML_TAG
%state SCRIPT_TAG
%state SCRIPT_CDATA
%state STYLE_TAG
%state STYLE_CDATA
%state NETTE_ATTR
%state NETTE_ATTR_VALUE
%state NETTE_ATTR_SQ
%state NETTE_ATTR_DQ
%state HTML_ATTR
%state HTML_ATTR_VALUE
%state HTML_ATTR_SQ
%state HTML_ATTR_DQ
%state HTML_COMMENT
%state SYNTAX_OFF
%state SYNTAX_DOUBLE
%state SYNTAX_OFF_NATTR
%state SYNTAX_DOUBLE_NATTR

%{
	private String lastNAttrName = null;
	private boolean syntaxOffPending = false;
	private boolean syntaxDoublePending = false;
	private String currentTagName = null;
	private String syntaxOffTagName = null;
	private int syntaxOffNestingDepth = 0;
%}

WHITE_SPACE=[ \t\r\n]+
MACRO_COMMENT = "{*" ~"*}"
MACRO_CLASSIC = "{" [^ \t\r\n'\"{}] ({MACRO_STRING} | "{" {MACRO_STRING}* "}")*  ("'" ("\\" [^] | [^'\\])* | "\"" ("\\" [^] | [^\"\\])*)? "}"?
SYNTAX_OFF_MACRO = "{syntax" [ \t]+ "off" [ \t]* "}"
SYNTAX_DOUBLE_MACRO = "{syntax" [ \t]+ "double" [ \t]* "}"
SYNTAX_CLOSE_MACRO = "{/syntax}"
DOUBLE_MACRO_COMMENT = "{{*" ~"*}}"
DOUBLE_MACRO_CLASSIC = "{{" [^ \t\r\n'\"{}] ({MACRO_STRING} | "{" {MACRO_STRING}* "}")*  ("'" ("\\" [^] | [^'\\])* | "\"" ("\\" [^] | [^\"\\])*)? "}}"?
DOUBLE_SYNTAX_CLOSE = "{{/syntax}}"
MACRO_STRING = {MACRO_STRING_SQ} | {MACRO_STRING_DQ} | {MACRO_STRING_UQ}
MACRO_STRING_SQ = "'" ("\\" [^] | [^'\\])* "'"
MACRO_STRING_DQ = "\"" ("\\" [^] | [^\"\\])* "\""
MACRO_STRING_UQ = [^'\"{}]

%%
<YYINITIAL> {
	[^] {
		rollbackMatch();
		pushState(HTML_TEXT);
	}
}

<HTML_TEXT, SCRIPT_TAG, SCRIPT_CDATA, STYLE_TAG, STYLE_CDATA, HTML_TAG, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT> {
	{MACRO_COMMENT} / [^]* {
		return T_MACRO_COMMENT;
	}

	{SYNTAX_OFF_MACRO} {
		pushState(SYNTAX_OFF);
		return T_MACRO_CLASSIC;
	}

	{SYNTAX_DOUBLE_MACRO} {
		pushState(SYNTAX_DOUBLE);
		return T_MACRO_CLASSIC;
	}

	{MACRO_CLASSIC} {
		return T_MACRO_CLASSIC;
	}
}

<HTML_TEXT> {
	"<!--" {
		pushState(HTML_COMMENT);
		return T_TEXT;
	}

	"<" / [a-zA-Z0-9:] {
		pushState(HTML_OPEN_TAG_OPEN);
		return T_HTML_OPEN_TAG_OPEN;
	}

	"</" / [a-zA-Z0-9:]  {
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_HTML_CLOSE_TAG_OPEN;
	}

	[^<{]+ {
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<HTML_OPEN_TAG_OPEN> {
	"script" / [^a-zA-Z0-9:] {
		currentTagName = "script";
		pushState(SCRIPT_TAG);
		return T_TEXT;
	}

	"style" / [^a-zA-Z0-9:] {
		currentTagName = "style";
		pushState(STYLE_TAG);
		return T_TEXT;
	}
}

<HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN> {
	[a-zA-Z0-9:]+ {
		if (yystate() == HTML_OPEN_TAG_OPEN) {
			currentTagName = yytext().toString().toLowerCase();
		}
		pushState(HTML_TAG);
		return T_TEXT;
	}
}

<SCRIPT_TAG> {
	"/>" {
		syntaxOffPending = false;
		syntaxDoublePending = false;
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		pushState(SCRIPT_CDATA);
		return T_TEXT;
	}

	">" {
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		if (syntaxOffPending) {
			syntaxOffPending = false;
			syntaxOffTagName = currentTagName;
			syntaxOffNestingDepth = 0;
			pushState(SYNTAX_OFF_NATTR);
		} else if (syntaxDoublePending) {
			syntaxDoublePending = false;
			syntaxOffTagName = currentTagName;
			syntaxOffNestingDepth = 0;
			pushState(SYNTAX_DOUBLE_NATTR);
		} else {
			pushState(SCRIPT_CDATA);
		}
		return T_TEXT;
	}
}

<SCRIPT_CDATA> {
	"</" / "script" [^a-zA-Z0-9:] {
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<STYLE_TAG> {
	"/>" {
		syntaxOffPending = false;
		syntaxDoublePending = false;
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		pushState(STYLE_CDATA);
		return T_TEXT;
	}

	">" {
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		if (syntaxOffPending) {
			syntaxOffPending = false;
			syntaxOffTagName = currentTagName;
			syntaxOffNestingDepth = 0;
			pushState(SYNTAX_OFF_NATTR);
		} else if (syntaxDoublePending) {
			syntaxDoublePending = false;
			syntaxOffTagName = currentTagName;
			syntaxOffNestingDepth = 0;
			pushState(SYNTAX_DOUBLE_NATTR);
		} else {
			pushState(STYLE_CDATA);
		}
		return T_TEXT;
	}
}

<STYLE_CDATA> {
	"</" / "style" [^a-zA-Z0-9:] {
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<HTML_TAG> {
	"/>" {
		syntaxOffPending = false;
		syntaxDoublePending = false;
		popState(HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN);
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		return T_HTML_OPEN_TAG_CLOSE;
	}

	">" {
		popState(HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN);
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		if (syntaxOffPending) {
			syntaxOffPending = false;
			syntaxOffTagName = currentTagName;
			syntaxOffNestingDepth = 0;
			pushState(SYNTAX_OFF_NATTR);
		} else if (syntaxDoublePending) {
			syntaxDoublePending = false;
			syntaxOffTagName = currentTagName;
			syntaxOffNestingDepth = 0;
			pushState(SYNTAX_DOUBLE_NATTR);
		}
		return T_HTML_TAG_CLOSE;
	}
}

<SCRIPT_TAG, STYLE_TAG, HTML_TAG> {
	"n:" [^ \t\r\n/>={]+ {
		lastNAttrName = yytext().toString().toLowerCase();
		pushState(NETTE_ATTR);
		return T_HTML_TAG_NATTR_NAME;
	}

	// TODO: missing !'n:' (sth. like [^n] | [n][^:])
	[^ \t\r\n/>={]+ {
		pushState(HTML_ATTR);
		return T_TEXT;
	}

	{WHITE_SPACE} {
		return T_TEXT;
	}

	[^] {
		return T_TEXT; // fallback
	}
}

<NETTE_ATTR> {
	"=" / [ \t\r\n]* [^ \t\r\n/>{] {
		pushState(NETTE_ATTR_VALUE);
		return T_HTML_TAG_ATTR_EQUAL_SIGN;
	}

	{WHITE_SPACE} {
		return T_WHITESPACE;
	}

	[^] {
		rollbackMatch();
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
	}
}

<NETTE_ATTR_VALUE> {
	['] {
		pushState(NETTE_ATTR_SQ);
		return T_HTML_TAG_ATTR_SQ;
	}

	[\"] {
		pushState(NETTE_ATTR_DQ);
		return T_HTML_TAG_ATTR_DQ;
	}

	[^ \t\r\n/>{'\"][^ \t\r\n/>{]* {
		popState(NETTE_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_MACRO_CONTENT;
	}

	{WHITE_SPACE} {
		return T_WHITESPACE;
	}

	[^] {
		rollbackMatch();
		popState(NETTE_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
	}
}

<NETTE_ATTR_SQ> {
	['] {
		popState(NETTE_ATTR_VALUE);
		popState(NETTE_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_HTML_TAG_ATTR_SQ;
	}

	[^']+ {
		if (lastNAttrName != null && lastNAttrName.equals("n:syntax")) {
			String val = yytext().toString().trim();
			if (val.equals("off")) {
				syntaxOffPending = true;
			} else if (val.equals("double")) {
				syntaxDoublePending = true;
			}
		}
		return T_MACRO_CONTENT;
	}
}

<NETTE_ATTR_DQ> {
	[\"] {
		popState(NETTE_ATTR_VALUE);
		popState(NETTE_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_HTML_TAG_ATTR_DQ;
	}

	[^\"]+ {
		if (lastNAttrName != null && lastNAttrName.equals("n:syntax")) {
			String val = yytext().toString().trim();
			if (val.equals("off")) {
				syntaxOffPending = true;
			} else if (val.equals("double")) {
				syntaxDoublePending = true;
			}
		}
		return T_MACRO_CONTENT;
	}
}

<HTML_ATTR> {
	"=" / [ \t\r\n]* [^ \t\r\n/>{] {
		pushState(HTML_ATTR_VALUE);
		return T_TEXT;
	}

	{WHITE_SPACE} {
		return T_TEXT;
	}

	[^] {
		rollbackMatch();
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
	}
}

<HTML_ATTR_VALUE> {
	['] {
		pushState(HTML_ATTR_SQ);
		return T_TEXT;
	}

	[\"] {
		pushState(HTML_ATTR_DQ);
		return T_TEXT;
	}

	[^ \t\r\n/>{'\"][^ \t\r\n/>{]* {
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_TEXT;
	}

	{WHITE_SPACE} {
		return T_TEXT;
	}

	[^] {
		rollbackMatch();
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
	}
}

<HTML_ATTR_SQ> {
	['] {
		popState(HTML_ATTR_VALUE);
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_TEXT;
	}

	[^'{]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}
}

<HTML_ATTR_DQ> {
	[\"] {
		popState(HTML_ATTR_VALUE);
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_TEXT;
	}

	[^\"{]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}
}

<HTML_COMMENT> {
	"-->" {
		popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}


<SYNTAX_DOUBLE> {
	{SYNTAX_CLOSE_MACRO} {
		popState(HTML_TEXT, SCRIPT_CDATA, STYLE_CDATA, HTML_TAG, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT);
		return T_MACRO_CLASSIC;
	}

	{DOUBLE_SYNTAX_CLOSE} {
		popState(HTML_TEXT, SCRIPT_CDATA, STYLE_CDATA, HTML_TAG, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT);
		return T_MACRO_CLASSIC;
	}

	{DOUBLE_MACRO_COMMENT} / [^]* {
		return T_MACRO_COMMENT;
	}

	{DOUBLE_MACRO_CLASSIC} {
		return T_MACRO_CLASSIC;
	}

	"<!--" {
		pushState(HTML_COMMENT);
		return T_TEXT;
	}

	"<" / [a-zA-Z0-9:] {
		pushState(HTML_OPEN_TAG_OPEN);
		return T_HTML_OPEN_TAG_OPEN;
	}

	"</" / [a-zA-Z0-9:] {
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_HTML_CLOSE_TAG_OPEN;
	}

	[^{<]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<SYNTAX_OFF> {
	{SYNTAX_CLOSE_MACRO} {
		popState(HTML_TEXT, SCRIPT_CDATA, STYLE_CDATA, HTML_TAG, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT);
		return T_MACRO_CLASSIC;
	}

	[^{]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}
}

<SYNTAX_OFF_NATTR> {
	"</" [a-zA-Z0-9:]+ {
		String tagName = yytext().toString().substring(2).toLowerCase();
		if (tagName.equals(syntaxOffTagName)) {
			if (syntaxOffNestingDepth == 0) {
				if (syntaxOffTagName.equals("script") || syntaxOffTagName.equals("style")) {
					// For script/style, mimic SCRIPT_CDATA/STYLE_CDATA close behavior:
					// return "</" as T_TEXT so the parser sees the same token pattern
					yypushback(yylength() - 2);
					popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
					pushState(HTML_CLOSE_TAG_OPEN);
					return T_TEXT;
				} else {
					yypushback(yylength());
					popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
				}
			} else {
				syntaxOffNestingDepth--;
				return T_TEXT;
			}
		} else {
			return T_TEXT;
		}
	}

	"<" [a-zA-Z0-9:]+ {
		String tagName = yytext().toString().substring(1).toLowerCase();
		if (tagName.equals(syntaxOffTagName)) {
			syntaxOffNestingDepth++;
		}
		return T_TEXT;
	}

	[^<]+ {
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<SYNTAX_DOUBLE_NATTR> {
	"</" [a-zA-Z0-9:]+ {
		String tagName = yytext().toString().substring(2).toLowerCase();
		if (tagName.equals(syntaxOffTagName)) {
			if (syntaxOffNestingDepth == 0) {
				if (syntaxOffTagName.equals("script") || syntaxOffTagName.equals("style")) {
					yypushback(yylength() - 2);
					popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
					pushState(HTML_CLOSE_TAG_OPEN);
					return T_TEXT;
				} else {
					yypushback(yylength());
					popState(HTML_TEXT, SYNTAX_DOUBLE, SYNTAX_DOUBLE_NATTR);
				}
			} else {
				syntaxOffNestingDepth--;
				yypushback(yylength() - 2);
				pushState(HTML_CLOSE_TAG_OPEN);
				return T_HTML_CLOSE_TAG_OPEN;
			}
		} else {
			yypushback(yylength() - 2);
			pushState(HTML_CLOSE_TAG_OPEN);
			return T_HTML_CLOSE_TAG_OPEN;
		}
	}

	"<" [a-zA-Z0-9:]+ {
		String tagName = yytext().toString().substring(1).toLowerCase();
		if (tagName.equals(syntaxOffTagName)) {
			syntaxOffNestingDepth++;
		}
		yypushback(yylength() - 1);
		pushState(HTML_OPEN_TAG_OPEN);
		return T_HTML_OPEN_TAG_OPEN;
	}

	"<!--" {
		pushState(HTML_COMMENT);
		return T_TEXT;
	}

	{DOUBLE_MACRO_COMMENT} / [^]* {
		return T_MACRO_COMMENT;
	}

	{DOUBLE_MACRO_CLASSIC} {
		return T_MACRO_CLASSIC;
	}

	[^{<]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<HTML_TEXT, HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN, HTML_TAG, SCRIPT_TAG, SCRIPT_CDATA, STYLE_TAG, STYLE_CDATA, NETTE_ATTR, NETTE_ATTR_SQ, NETTE_ATTR_DQ, HTML_ATTR, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT, SYNTAX_OFF, SYNTAX_DOUBLE, SYNTAX_OFF_NATTR, SYNTAX_DOUBLE_NATTR> {
	[^] {
		// throw new RuntimeException('Lexer failed');
		return com.intellij.psi.TokenType.BAD_CHARACTER;
	}
}
