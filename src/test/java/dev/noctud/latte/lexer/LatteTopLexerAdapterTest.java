package dev.noctud.latte.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.util.Pair;
import org.junit.Test;

import static dev.noctud.latte.Assert.assertTokens;
import static dev.noctud.latte.psi.LatteTypes.*;

public class LatteTopLexerAdapterTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testSimple() {
		Lexer lexer = new LatteTopLexerAdapter();

		lexer.start("");
		assertTokens(lexer, new Pair[] {});

		lexer.start("abc<strong>def</strong>ghi");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_TEXT, "abc"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "strong"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "def"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "strong"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "ghi"),
		});

		lexer.start("<strong a1='v1' a2=\"v2\" a3=v3>abc</strong>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "strong a1='v1' a2=\"v2\" a3=v3"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "abc"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "strong"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("{maAro}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{maAro}"),
		});

		lexer.start("{ macro }");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_TEXT, "{ macro }"),
		});

		lexer.start("{macro}abc{/}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{macro}"),
			Pair.create(T_TEXT, "abc"),
			Pair.create(T_MACRO_CLASSIC, "{/}"),
		});

		lexer.start("abc{macro}def{/macro}ghi");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_TEXT, "abc"),
			Pair.create(T_MACRO_CLASSIC, "{macro}"),
			Pair.create(T_TEXT, "def"),
			Pair.create(T_MACRO_CLASSIC, "{/macro}"),
			Pair.create(T_TEXT, "ghi"),
		});

		lexer.start("{? $f = function() { } }");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{? $f = function() { } }"),
		});

		lexer.start("{={a|b}}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{={a|b}}"),
		});

		lexer.start("<div {if $abc}def{/if}>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_MACRO_CLASSIC, "{if $abc}"),
			Pair.create(T_TEXT, "def"),
			Pair.create(T_MACRO_CLASSIC, "{/if}"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("<div attr='{$val}'>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div attr='"),
			Pair.create(T_MACRO_CLASSIC, "{$val}"),
			Pair.create(T_TEXT, "'"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("<div attr='abc{$val}def'>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div attr='abc"),
			Pair.create(T_MACRO_CLASSIC, "{$val}"),
			Pair.create(T_TEXT, "def'"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("<div attr='abc{ $val }def'>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div attr='abc{ $val }def'"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("<div n:attr='{$val}'>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:attr"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_SQ, "'"),
			Pair.create(T_MACRO_CONTENT, "{$val}"),
			Pair.create(T_HTML_TAG_ATTR_SQ, "'"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("A<!-- {$b} <el> -->C");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_TEXT, "A<!-- "),
			Pair.create(T_MACRO_CLASSIC, "{$b}"),
			Pair.create(T_TEXT, " <el> -->C"),
		});

		lexer.start("A{* {$var} * <el> *}B{*C}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_TEXT, "A"),
			Pair.create(T_MACRO_COMMENT, "{* {$var} * <el> *}"),
			Pair.create(T_TEXT, "B"),
			Pair.create(T_MACRO_CLASSIC, "{*C}"),
		});

		lexer.start("<script><el>{$var}</style></script><el>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "script><el>"),
			Pair.create(T_MACRO_CLASSIC, "{$var}"),
			Pair.create(T_TEXT, "</style></script"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "el"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("<style><el>{$var}</script></style><el>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "style><el>"),
			Pair.create(T_MACRO_CLASSIC, "{$var}"),
			Pair.create(T_TEXT, "</script></style"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "el"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		lexer.start("<script><el>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "script><el>"),
		});

		// edge
		lexer.start("<e1 a1=\"A{if $cond}<e2 a2=\"B{if $cond}INNER{/if}C\">{/if}D\">");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "e1 a1=\"A"),
			Pair.create(T_MACRO_CLASSIC, "{if $cond}"),
			Pair.create(T_TEXT, "<e2 a2=\"B"),
			Pair.create(T_MACRO_CLASSIC, "{if $cond}"),
			Pair.create(T_TEXT, "INNER"),
			Pair.create(T_MACRO_CLASSIC, "{/if}"),
			Pair.create(T_TEXT, "C\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_MACRO_CLASSIC, "{/if}"),
			Pair.create(T_TEXT, "D\">"),
		});

		lexer.start("{* ' *}A{* ' *}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_COMMENT, "{* ' *}"),
			Pair.create(T_TEXT, "A"),
			Pair.create(T_MACRO_COMMENT, "{* ' *}"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedHtmlTags() {
		Lexer lexer = new LatteTopLexerAdapter();

		lexer.start("<div><span>text</span></div>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "span"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "text"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "span"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSelfClosingTag() {
		Lexer lexer = new LatteTopLexerAdapter();

		lexer.start("<br/>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "br"),
			Pair.create(T_HTML_OPEN_TAG_CLOSE, "/>"),
		});

		lexer.start("<input type=\"text\" />");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "input type=\"text\" "),
			Pair.create(T_HTML_OPEN_TAG_CLOSE, "/>"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSyntaxOff() {
		Lexer lexer = new LatteTopLexerAdapter();

		// Basic syntax off - macros inside become plain text
		lexer.start("{syntax off}{$var}{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax off}"),
			Pair.create(T_TEXT, "{$var}"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// Syntax off with surrounding content
		lexer.start("A{syntax off}B{$var}C{/syntax}D");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_TEXT, "A"),
			Pair.create(T_MACRO_CLASSIC, "{syntax off}"),
			Pair.create(T_TEXT, "B{$var}C"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
			Pair.create(T_TEXT, "D"),
		});

		// Comments also suppressed inside syntax off
		lexer.start("{syntax off}{* comment *}{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax off}"),
			Pair.create(T_TEXT, "{* comment *}"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// Multiple macros suppressed
		lexer.start("{syntax off}{if true}hello{/if}{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax off}"),
			Pair.create(T_TEXT, "{if true}hello{/if}"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// HTML tags inside syntax off are plain text
		lexer.start("{syntax off}<div class=\"test\">{$var}</div>{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax off}"),
			Pair.create(T_TEXT, "<div class=\"test\">{$var}</div>"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// Unclosed syntax off - consumes to EOF
		lexer.start("{syntax off}{$var}text");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax off}"),
			Pair.create(T_TEXT, "{$var}text"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSyntaxDouble() {
		Lexer lexer = new LatteTopLexerAdapter();

		// Single braces are plain text, double braces are macros
		// Closing with single-brace {/syntax} (Latte uses original delimiters for close)
		lexer.start("{syntax double}{$var}{{$var}}{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax double}"),
			Pair.create(T_TEXT, "{$var}"),
			Pair.create(T_MACRO_CLASSIC, "{{$var}}"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// Also works with double-brace close
		lexer.start("{syntax double}{{$var}}{{/syntax}}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax double}"),
			Pair.create(T_MACRO_CLASSIC, "{{$var}}"),
			Pair.create(T_MACRO_CLASSIC, "{{/syntax}}"),
		});

		// Double-brace pair macros
		lexer.start("{syntax double}{{if $cond}}hello{{/if}}{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax double}"),
			Pair.create(T_MACRO_CLASSIC, "{{if $cond}}"),
			Pair.create(T_TEXT, "hello"),
			Pair.create(T_MACRO_CLASSIC, "{{/if}}"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// Double-brace comments
		lexer.start("{syntax double}{{* comment *}}text{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax double}"),
			Pair.create(T_MACRO_COMMENT, "{{* comment *}}"),
			Pair.create(T_TEXT, "text"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// JavaScript-like content with single braces is preserved as text
		lexer.start("{syntax double}var x = {a: 1, b: 2};{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax double}"),
			Pair.create(T_TEXT, "var x = {a: 1, b: 2};"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});

		// HTML tags and n:attributes work inside syntax double
		lexer.start("{syntax double}<span n:if=\"$show\">{{$var}}</span>{/syntax}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_CLASSIC, "{syntax double}"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "span "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:if"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "$show"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_MACRO_CLASSIC, "{{$var}}"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "span"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_MACRO_CLASSIC, "{/syntax}"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNSyntaxOff() {
		Lexer lexer = new LatteTopLexerAdapter();

		// Basic n:syntax="off" - macros inside become plain text
		lexer.start("<div n:syntax=\"off\">{$var}</div>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "off"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "{$var}"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		// n:syntax with other attributes
		lexer.start("<div class=\"test\" n:syntax=\"off\">{if $x}y{/if}</div>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div class=\"test\" "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "off"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "{if $x}y{/if}"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		// n:syntax with nested same-name tags
		lexer.start("<div n:syntax=\"off\"><div>inner</div></div>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "off"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "<div>inner</div>"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		// Self-closing tag with n:syntax should not enter syntax off mode
		lexer.start("<br n:syntax=\"off\" />{$var}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "br "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "off"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_TEXT, " "),
			Pair.create(T_HTML_OPEN_TAG_CLOSE, "/>"),
			Pair.create(T_MACRO_CLASSIC, "{$var}"),
		});

		// n:syntax="off" on <script> tag - macros suppressed inside, next tag normal
		lexer.start("<script n:syntax=\"off\">var x = {a: 1};</script><script>{$var}</script>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "script "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "off"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			// </script close uses T_TEXT pattern (like SCRIPT_CDATA), not T_HTML_CLOSE_TAG_OPEN
			Pair.create(T_TEXT, ">var x = {a: 1};</script"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			// Second script tag works normally with macro parsing
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "script>"),
			Pair.create(T_MACRO_CLASSIC, "{$var}"),
			Pair.create(T_TEXT, "</script"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNSyntaxDouble() {
		Lexer lexer = new LatteTopLexerAdapter();

		// n:syntax="double" - single braces are text, double braces are macros
		lexer.start("<div n:syntax=\"double\">{$test}{{$test}}</div>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "double"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_TEXT, "{$test}"),
			Pair.create(T_MACRO_CLASSIC, "{{$test}}"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});

		// n:attributes on tags inside n:syntax="double" should work
		lexer.start("<div n:syntax=\"double\"><span n:if=\"$show\">{{$var}}</span></div>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:syntax"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "double"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "span "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:if"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_CONTENT, "$show"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_MACRO_CLASSIC, "{{$var}}"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "span"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
			Pair.create(T_TEXT, "div"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMacroInAttribute() {
		Lexer lexer = new LatteTopLexerAdapter();

		lexer.start("<div class=\"{$cls}\">");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div class=\""),
			Pair.create(T_MACRO_CLASSIC, "{$cls}"),
			Pair.create(T_TEXT, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testHtml() {
		Lexer lexer = new LatteTopLexerAdapter();

		lexer.start("");
		assertTokens(lexer, new Pair[] {});

		lexer.start("abc<strong>def</strong>ghi");
		assertTokens(lexer, new Pair[] {
				Pair.create(T_TEXT, "abc"),
				Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
				Pair.create(T_TEXT, "strong"),
				Pair.create(T_HTML_TAG_CLOSE, ">"),
				Pair.create(T_TEXT, "def"),
				Pair.create(T_HTML_CLOSE_TAG_OPEN, "</"),
				Pair.create(T_TEXT, "strong"),
				Pair.create(T_HTML_TAG_CLOSE, ">"),
				Pair.create(T_TEXT, "ghi"),
		});

		lexer.start("abc<img src=\"\" />ghi");
		assertTokens(lexer, new Pair[] {
				Pair.create(T_TEXT, "abc"),
				Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
				Pair.create(T_TEXT, "img src=\"\" "),
				Pair.create(T_HTML_OPEN_TAG_CLOSE, "/>"),
				Pair.create(T_TEXT, "ghi"),
		});

		lexer.start("abc<style></style>");
		assertTokens(lexer, new Pair[] {
				Pair.create(T_TEXT, "abc"),
				Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
				Pair.create(T_TEXT, "style></style"),
				Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}
}
