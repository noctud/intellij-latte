package dev.noctud.latte.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.util.Pair;
import org.junit.Test;

import static dev.noctud.latte.Assert.assertTokens;
import static dev.noctud.latte.psi.LatteTypes.*;

public class LatteLexerTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testBasic() throws Exception {
		Lexer lexer = new LatteLexer();

		lexer.start("<e>{if XYZ}B{/if}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "e"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_NAME, "if"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_PHP_IDENTIFIER, "XYZ"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
			Pair.create(T_TEXT, "B"),
			Pair.create(T_MACRO_CLOSE_TAG_OPEN, "{/"),
			Pair.create(T_MACRO_NAME, "if"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});

		lexer.start("{if XYZ}B{/if}<e>");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_NAME, "if"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_PHP_IDENTIFIER, "XYZ"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
			Pair.create(T_TEXT, "B"),
			Pair.create(T_MACRO_CLOSE_TAG_OPEN, "{/"),
			Pair.create(T_MACRO_NAME, "if"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "e"),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testVariableOutput() throws Exception {
		Lexer lexer = new LatteLexer();

		lexer.start("{$variable}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_ARGS_VAR, "$variable"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedMacros() throws Exception {
		Lexer lexer = new LatteLexer();

		lexer.start("{if $a}{$b}{/if}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_NAME, "if"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_MACRO_ARGS_VAR, "$a"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_ARGS_VAR, "$b"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
			Pair.create(T_MACRO_CLOSE_TAG_OPEN, "{/"),
			Pair.create(T_MACRO_NAME, "if"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNAttributes() throws Exception {
		Lexer lexer = new LatteLookAheadLexer(new LatteLexer());

		lexer.start("<div n:if=\"$show\" n:class=\"$cls\">");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_HTML_OPEN_TAG_OPEN, "<"),
			Pair.create(T_TEXT, "div "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:if"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_ARGS_VAR, "$show"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_TEXT, " "),
			Pair.create(T_HTML_TAG_NATTR_NAME, "n:class"),
			Pair.create(T_HTML_TAG_ATTR_EQUAL_SIGN, "="),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_MACRO_ARGS_VAR, "$cls"),
			Pair.create(T_HTML_TAG_ATTR_DQ, "\""),
			Pair.create(T_HTML_TAG_CLOSE, ">"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMacroComment() throws Exception {
		Lexer lexer = new LatteTopLexerAdapter();

		lexer.start("{* comment *}text");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_COMMENT, "{* comment *}"),
			Pair.create(T_TEXT, "text"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTernaryOperator() throws Exception {
		Lexer lexer = new LatteLexer();

		lexer.start("{$a ? $b : $c}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_ARGS_VAR, "$a"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_PHP_NULL_MARK, "?"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_MACRO_ARGS_VAR, "$b"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_PHP_COLON, ":"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_MACRO_ARGS_VAR, "$c"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testStringEscaping() throws Exception {
		Lexer lexer = new LatteLexer();

		lexer.start("{= \"hello \\\"world\\\"\"}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_SHORTNAME, "="),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_PHP_DOUBLE_QUOTE_LEFT, "\""),
			Pair.create(T_MACRO_ARGS_STRING, "hello \\\"world\\\""),
			Pair.create(T_PHP_DOUBLE_QUOTE_RIGHT, "\""),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFileImport() throws Exception {
		Lexer lexer = new LatteLexer();

		lexer.start("{include ../@Layout/test.latte}");
		assertTokens(lexer, new Pair[] {
				Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
				Pair.create(T_MACRO_NAME, "include"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_FILE_PATH, "../@Layout/test.latte"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});

		lexer.start("{include '../@Layout/test.latte'}");
		assertTokens(lexer, new Pair[] {
				Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
				Pair.create(T_MACRO_NAME, "include"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_SINGLE_QUOTE_LEFT, "'"),
				Pair.create(T_FILE_PATH, "../@Layout/test.latte"),
				Pair.create(T_PHP_SINGLE_QUOTE_RIGHT, "'"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});

		lexer.start("{if $test}{include \"../@Layout/test.latte\"}{/if}");
		assertTokens(lexer, new Pair[] {
				Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
				Pair.create(T_MACRO_NAME, "if"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_MACRO_ARGS_VAR, "$test"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
				Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
				Pair.create(T_MACRO_NAME, "include"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_DOUBLE_QUOTE_LEFT, "\""),
				Pair.create(T_FILE_PATH, "../@Layout/test.latte"),
				Pair.create(T_PHP_DOUBLE_QUOTE_RIGHT, "\""),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
				Pair.create(T_MACRO_CLOSE_TAG_OPEN, "{/"),
				Pair.create(T_MACRO_NAME, "if"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}
}
