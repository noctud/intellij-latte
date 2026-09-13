package dev.noctud.latte.parser;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.settings.LatteSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * A block comment that ends a tag, which Latte accepts everywhere and this used to call an error.
 *
 * <p>The comment was never the problem; its end was. A tag ending in <code>*&#47;}</code> was read
 * as a comment that never closed followed by <code>/}</code>, which is how a tag says it closes
 * itself - so the tag ended in the middle of the comment and the parser reported what it was still
 * waiting for. It only happened when the comment was the last thing in the tag: put anything after
 * it and the closing brace stood on its own.
 *
 * <p>Checked against Latte 3.1: every template here compiles, so every one of them has to be quiet.
 */
public class BlockCommentInTagTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        return "";
    }

    @Test
    public void testATagMayEndWithABlockComment() {
        assertQuiet("{var $a = 1 /* note */}");
        assertQuiet("{do $a = 1 /* note */}");
        assertQuiet("{=1 /* note */}");
        assertQuiet("{php $a = 1 /* note */}");
        assertQuiet("{if true /* note */}x{/if}");
    }

    /** These were already quiet. They are here so that fixing the end cannot break the start. */
    @Test
    public void testATagMayAlsoBeginWithABlockComment() {
        assertQuiet("{var /* note */ $a = 1}");
        assertQuiet("{do /* note */ $a = 1}");
        assertQuiet("{if /* note */ true}x{/if}");
        assertQuiet("{foreach /* note */ [1] as $i}x{/foreach}");
    }

    /**
     * The counterweight. A tag really can close itself with <code>/}</code>, and reading a comment
     * before it must not take that away - otherwise the fix would trade one wrong answer for
     * another.
     */
    @Test
    public void testATagCanStillCloseItself() {
        PsiFile file = parseFile("SelfClosing.latte", "{block foo /}");

        Assert.assertEquals(List.of(), errorsIn(file));
        Assert.assertTrue(
            "a tag closing itself has to keep the /} that says so",
            file.getFirstChild().getText().contains("/}")
        );
    }

    /**
     * A comment nobody closed stays what it was: text. The rule needs its ending to match, so an
     * opening on its own leaves the tag exactly as unreadable as it is today, rather than eating
     * the rest of the file looking for one.
     */
    @Test
    public void testAnUnclosedBlockCommentDoesNotSwallowTheFile() {
        PsiFile file = parseFile("Unclosed.latte", "{var $a = 1 /* note}\n<p>after</p>");

        Assert.assertTrue(
            "the text after the tag has to survive an unterminated comment",
            file.getText().contains("<p>after</p>")
        );
    }

    private void assertQuiet(String template) {
        PsiFile file = parseFile("Comment.latte", template);

        Assert.assertEquals("Latte compiles " + template + ", so it must not be reported", List.of(), errorsIn(file));
    }

    private static List<String> errorsIn(PsiFile file) {
        List<String> errors = new ArrayList<>();
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PsiErrorElement) {
                    errors.add(((PsiErrorElement) element).getErrorDescription());
                }
                super.visitElement(element);
            }
        });
        return errors;
    }
}
