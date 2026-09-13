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
 * How deep a tag may nest braces before the lexer stops following them.
 *
 * The lexer closes a tag once the braces inside it get deeper than a fixed number, because
 * following a runaway brace to the end of a broken file hands the parser one tag holding the whole
 * template - and that it parses in quadratic time. The guard is against that parse and nothing
 * else: lexing itself is linear with it and without it.
 *
 * <p>The number was 16, which is far above what a template writes and not far above what a
 * hand-written {@code {php}} block may legitimately reach. At 16, seventeen levels of nesting -
 * unusual PHP, but correct PHP - was lexed as if the tag had ended early. This asserts the depth
 * that used to be wrong is right now, and that the guard is still there.
 */
public class DeepNestingTest extends BasePsiParsingTestCase {

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

    /** Twenty levels: above the old limit, far below the new one. */
    @Test
    public void testATagNestingDeeperThanTheOldLimitIsStillOneTag() {
        String template = nested(20);

        PsiFile file = parseFile("DeepNesting.latte", template);

        Assert.assertEquals(
            "A {php} block twenty braces deep is correct PHP and has to parse as one tag",
            List.of(),
            errorsIn(file)
        );
        Assert.assertEquals(
            "the whole tag has to be one element, not a tag that ended early plus loose text",
            template.trim(),
            file.getFirstChild().getText().trim()
        );
    }

    /**
     * The counterweight. Without a limit at all, four thousand braces take twelve seconds to parse
     * - so the limit has to still be there, and something deep enough has to still hit it.
     */
    @Test
    public void testTheGuardIsStillThere() {
        PsiFile file = parseFile("TooDeep.latte", nested(400));

        Assert.assertNotEquals(
            "a tag nesting four hundred braces deep must not be followed to the end",
            nested(400).trim(),
            file.getFirstChild().getText().trim()
        );
    }

    private static String nested(int depth) {
        return "{php $a = " + "[".repeat(0) + "{".repeat(depth) + "}".repeat(depth) + "}\n";
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
