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
