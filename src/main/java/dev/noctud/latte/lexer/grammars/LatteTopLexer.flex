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
%state NETTE_ATTR_CURLY
%state HTML_ATTR
%state HTML_ATTR_VALUE
%state HTML_ATTR_SQ
%state HTML_ATTR_DQ
%state HTML_COMMENT
%state SYNTAX_OFF
%state SYNTAX_DOUBLE
%state SYNTAX_OFF_NATTR
%state SYNTAX_DOUBLE_NATTR
%state MACRO_BODY

%{
	private String lastNAttrName = null;
	private boolean syntaxOffPending = false;
	private boolean syntaxDoublePending = false;
	private String currentTagName = null;
	private String syntaxOffTagName = null;
	private int syntaxOffNestingDepth = 0;
	private int macroNestingDepth = 0;

	/**
	 * Depth at which a tag is closed where it stands instead of being followed further. Real code
	 * nests a handful of levels inside one tag, so this is never reached by a template; a run
	 * deeper than this only happens on broken input, where following it to the end of the file
	 * would hand the parser a single tag holding the whole file - which it parses in quadratic
	 * time. Closing early puts such input back where it was before braces were counted at all.
	 *
	 * The number is a guard against that quadratic parse and nothing else - lexing is linear with
	 * it and without it (measured: 0.1-0.2 ms over 4 000 braces either way). It was 16, which is
	 * comfortably above what a template writes and comfortably below what a hand-written {php}
	 * block may legitimately reach: seventeen levels of nesting is unusual PHP but it is correct
	 * PHP, and at 16 it was lexed wrongly. Raising it to 128 keeps the guard - without one, 4 000
	 * braces take twelve seconds - and buys back the correctness in between.
	 */
	private static final int MAX_MACRO_NESTING_DEPTH = 128;
%}

WHITE_SPACE=[ \t\r\n]+
MACRO_COMMENT = "{*" ~"*}"
MACRO_OPEN = "{" [^ \t\r\n'\"{}]
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
PHP_BLOCK_COMMENT = "/*" ~"*/"
// Latte itself knows no line comment inside a tag - its TagLexer matches only /* ... */, and a
// '//' or a '#' there is a syntax error. This is not modelling one, then: it is keeping a quote
// written inside such a run from reaching across the tag. The closing brace is left out of the
// run on purpose, so that the run can never take the brace the tag ends at - which is what tells
// this apart from a real line comment, and what keeps {link //Presenter:action}, {include #block}
// and a fragment written as {link Presenter:action#anchor} ending where they are written to end.
PHP_LINE_COMMENT = ("//" | "#") [^\r\n}]*

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

	// Only the opening of the tag is matched here; MACRO_BODY reads the rest and returns the
	// token. The action returns nothing, so the tag keeps growing until the body closes it - the
	// resulting T_MACRO_CLASSIC spans everything matched in between.
	{MACRO_OPEN} {
		macroNestingDepth = 1;
		pushState(MACRO_BODY);
	}
}

// Body of a classic macro. Braces are counted rather than matched by a pattern, so a block nested
// to any depth - a {php} body, a closure inside a closure - stays part of the tag. A quoted
// literal is taken whole, which keeps a brace inside it from being counted at all.
<MACRO_BODY> {
	// A PHP block comment is content as a whole, before anything in it is read as PHP. An
	// apostrophe in it is ordinary English - "isn't", "don't" - and taken as the start of a literal
	// it paired with the next quote written anywhere below, across the brace closing the tag: from
	// that comment down the template stopped being HTML and became one tag.
	{PHP_BLOCK_COMMENT} {
	}

	// The same containment for a run started by '//' or '#'. An apostrophe in "it isn't reset"
	// paired with the next quote written anywhere below, and that pairing spanned the brace
	// closing the tag: from that line down the template stopped being HTML and became one tag.
	// Latte reports one error at the apostrophe and the tag still ends; this keeps the damage
	// that small instead of letting one mistyped line take the rest of the file with it.
	{PHP_LINE_COMMENT} {
	}

	{MACRO_STRING_SQ} | {MACRO_STRING_DQ} {
	}

	// A quote that opens no complete literal - the state the editor lexes while one is being
	// typed. It is content on its own, so that the braces after it keep being counted and the tag
	// still ends where it is written to end; a tag with no closing brace after it runs to the end
	// of the input as any unclosed tag does. Read instead as a literal running to the end of the
	// input, it took the rest of the template with it. This is the rule the macro content lexer
	// is built on as well.
	['\"] {
	}

	"{" {
		macroNestingDepth++;
		if (macroNestingDepth > MAX_MACRO_NESTING_DEPTH) {
			popState();
			return T_MACRO_CLASSIC;
		}
	}

	"}" {
		macroNestingDepth--;
		if (macroNestingDepth == 0) {
			popState();
			return T_MACRO_CLASSIC;
		}
	}

	// An unclosed tag ends with the file; the editor sees that on every keystroke. The state is
	// left before returning so that the end of input is reported once and the scan terminates.
	<<EOF>> {
		popState();
		return T_MACRO_CLASSIC;
	}

	// The slash and the hash are left out so that a match can start on one: taken as an ordinary
	// character either was swallowed by the run before it and the comment above never got the
	// chance to open.
	[^{}'\"/#]+ {
	}

	// A slash that opens no comment - a division, a path written in a link tag.
	"/" {
	}

	// A hash that opens no run - it is left in the tag as ordinary content the way it always was.
	"#" {
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
	"=" / [ \t\r\n]* [^ \t\r\n/>] {
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

	"{" {
		pushState(NETTE_ATTR_CURLY);
		return T_HTML_TAG_ATTR_CURLY_LEFT;
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

<NETTE_ATTR_CURLY> {
	"}" {
		popState(NETTE_ATTR_VALUE);
		popState(NETTE_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_HTML_TAG_ATTR_CURLY_RIGHT;
	}

	[^}]+ {
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

<HTML_TEXT, HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN, HTML_TAG, SCRIPT_TAG, SCRIPT_CDATA, STYLE_TAG, STYLE_CDATA, NETTE_ATTR, NETTE_ATTR_SQ, NETTE_ATTR_DQ, NETTE_ATTR_CURLY, HTML_ATTR, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT, SYNTAX_OFF, SYNTAX_DOUBLE, SYNTAX_OFF_NATTR, SYNTAX_DOUBLE_NATTR> {
	[^] {
		// throw new RuntimeException('Lexer failed');
		return com.intellij.psi.TokenType.BAD_CHARACTER;
	}
}
