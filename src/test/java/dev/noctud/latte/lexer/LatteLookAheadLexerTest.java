package dev.noctud.latte.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.util.Pair;
import org.junit.Test;

import static dev.noctud.latte.Assert.assertTokens;
import static dev.noctud.latte.psi.LatteTypes.*;

public class LatteLookAheadLexerTest {
	@Test
	@SuppressWarnings("unchecked")
	public void testLinkMacroLexer() {
		Lexer lexer = new LatteLookAheadLexer(new LatteLexer());

		lexer.start("{link default}");
		assertTokens(lexer, new Pair[] {
			Pair.create(T_MACRO_OPEN_TAG_OPEN, "{"),
			Pair.create(T_MACRO_NAME, "link"),
			Pair.create(T_WHITESPACE, " "),
			Pair.create(T_LINK, "default"),
			Pair.create(T_MACRO_TAG_CLOSE, "}"),
		});
	}
}
