package dev.noctud.latte.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.util.Pair;
import org.junit.Test;

import static dev.noctud.latte.Assert.assertTokens;
import static dev.noctud.latte.psi.LatteTypes.*;

public class LatteMacroContentLexerAdapterTest {

	@Test
	@SuppressWarnings("unchecked")
	public void testMacroLexer() {
		Lexer lexer = new LatteMacroContentLexerAdapter();

		lexer.start(" a ");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "a"),
				Pair.create(T_WHITESPACE, " "),
		});

		lexer.start("$var");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "$var"),
		});

		lexer.start("a()");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "a()"),
		});

		lexer.start("a::b");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "a"),
				Pair.create(T_MACRO_ARGS, "::"),
				Pair.create(T_PHP_CONTENT, "b"),
		});

		lexer.start("a\\b");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "a\\b"),
		});

		lexer.start("$var|noescape");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "$var|noescape"),
		});

		lexer.start("|test");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "|test"),
		});

		lexer.start(" function() { } ");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "function() { } "),
		});

		lexer.start("1");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "1"),
		});

		lexer.start("1a");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "1a"),
		});

		lexer.start("a1");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_PHP_CONTENT, "a1"),
		});
	}

	/**
	 * Two complete literals in one tag can also be paired shifted by one quote: the closing quote
	 * of the first read as an opening one and the opening quote of the second as its closing one.
	 * Such a pairing spans everything in between - newlines and braces included - so it is the
	 * longer match and wins, and the brace it swallows is never counted. The body below then ends
	 * at the brace that closes the loop instead of the one that closes the tag.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testTwoLiteralsDoNotPairUpAcrossTheBraceBetweenThem() {
		Lexer lexer = new LatteMacroContentLexerAdapter();

		lexer.start("\n\t$a = ['k' => 1];\n\tforeach ($x as $i) {\n\t\t$b = ['k' => 2];\n\t}\n}");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, "\n\t"),
				Pair.create(T_PHP_CONTENT, "$a = ['k' => 1];"),
				Pair.create(T_WHITESPACE, "\n\t"),
				Pair.create(T_PHP_CONTENT, "foreach"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "($x as $i) {\n\t\t$b = ['k' => 2];\n\t}"),
				Pair.create(T_WHITESPACE, "\n"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});

		lexer.start("\n\t$a = [\"k\" => 1];\n\tforeach ($x as $i) {\n\t\t$b = [\"k\" => 2];\n\t}\n}");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, "\n\t"),
				Pair.create(T_PHP_CONTENT, "$a = [\"k\" => 1];"),
				Pair.create(T_WHITESPACE, "\n\t"),
				Pair.create(T_PHP_CONTENT, "foreach"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "($x as $i) {\n\t\t$b = [\"k\" => 2];\n\t}"),
				Pair.create(T_WHITESPACE, "\n"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}

	/**
	 * The same character class decides what a braced group is made of, so a brace inside a literal
	 * written below the tag's own level was counted as well. Here the tag closed at the brace the
	 * literal holds.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testBraceInsideALiteralIsNotCountedAtAnyDepth() {
		Lexer lexer = new LatteMacroContentLexerAdapter();

		lexer.start(" if ($x) { echo '}'; } }");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "if"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "($x) { echo '}'; } "),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});

		lexer.start(" if ($x) { echo \"{\"; } }");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "if"),
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "($x) { echo \"{\"; } "),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}

	/**
	 * A quote that has no closing one yet is what the editor lexes on every keystroke while a
	 * literal is being typed. It stays content, and the braces after it keep being counted.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testUnclosedLiteralStaysContent() {
		Lexer lexer = new LatteMacroContentLexerAdapter();

		lexer.start(" $a = 'oops}");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "$a = 'oops"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});

		lexer.start(" $a = \"oops}");
		assertTokens(lexer, new Pair[]{
				Pair.create(T_WHITESPACE, " "),
				Pair.create(T_PHP_CONTENT, "$a = \"oops"),
				Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}
}
