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
 * Where a tag ends when a brace inside it belongs to its content. Latte compiles every template
 * here; ending the tag at the wrong brace spills the rest of the tag into the surrounding HTML,
 * which the parser then reports.
 */
public class TagEndTest extends BasePsiParsingTestCase {

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

    /** Latte skips over a literal when it looks for the closing brace, so an unbalanced one inside is content. */
    @Test
    public void testABraceInsideALiteralDoesNotEndTheTag() {
        assertQuiet("<p>{= 'a string with a closing brace } inside it'}</p>");
        assertQuiet("<p>{= \"a string with a closing brace } inside it\"}</p>");
        assertQuiet("<p>{= 'a string with an opening brace { inside it'}</p>");
    }

    /** A block inside a block, spread over lines or on one, and several levels deep. */
    @Test
    public void testABlockInsideABlockStaysInsideTheTag() {
        assertQuiet("{php\n"
            + "\t$grouped = [];\n"
            + "\tforeach ($items as $item) {\n"
            + "\t\tif ($item->isVisible()) {\n"
            + "\t\t\t$grouped[] = $item;\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}\n"
            + "<p>{count($grouped)}</p>");
        assertQuiet("{php $factory = function () { return ['make' => function () { return 1; }]; };}");
        assertQuiet("{php\n"
            + "\tforeach ($items as $item) {\n"
            + "\t\tif ($item->isVisible()) {\n"
            + "\t\t\tforeach ($item->getTags() as $tag) {\n"
            + "\t\t\t\tif ($tag !== null) {\n"
            + "\t\t\t\t\t$grouped[$tag] = $item;\n"
            + "\t\t\t\t}\n"
            + "\t\t\t}\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}\n"
            + "<p>{count($grouped)}</p>");
    }

    private void assertQuiet(String template) {
        PsiFile file = parseFile("TagEnd.latte", template);

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
