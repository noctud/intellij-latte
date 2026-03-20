package dev.noctud.latte.formatter;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.psi.LattePairMacro;
import dev.noctud.latte.settings.LatteSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests PSI tree structure for deeply nested templates.
 * Verifies that the parser correctly recognizes pair macros at any nesting depth,
 * which is a prerequisite for correct formatting.
 *
 * Note: actual reformat behavior cannot be tested with ParsingTestCase
 * (requires BasePlatformTestCase with full PhpStorm sandbox).
 */
public class LatteFormatterTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        URL url = getClass().getClassLoader().getResource("data/formatter");
        assert url != null;
        return url.getFile();
    }

    @Test
    public void testDeeplyNestedParsesWithoutErrors() throws IOException {
        PsiFile file = parseFile("DeeplyNested", loadFile("DeeplyNested.latte"));

        List<String> errors = new ArrayList<>();
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PsiErrorElement) {
                    errors.add(((PsiErrorElement) element).getErrorDescription());
                }
                super.visitElement(element);
            }
        });

        assertTrue("PSI tree should have no errors but found: " + errors, errors.isEmpty());
    }

    @Test
    public void testAllBlocksArePaired() throws IOException {
        PsiFile file = parseFile("DeeplyNested", loadFile("DeeplyNested.latte"));

        List<String> pairedMacros = new ArrayList<>();
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof LattePairMacro) {
                    LattePairMacro pair = (LattePairMacro) element;
                    if (pair.getMacroOpenTag() != null) {
                        pairedMacros.add(pair.getMacroOpenTag().getMacroName());
                    }
                }
                super.visitElement(element);
            }
        });

        assertTrue("block should be paired", pairedMacros.contains("block"));
        assertTrue("snippet should be paired", pairedMacros.contains("snippet"));
        assertTrue("foreach should be paired", pairedMacros.contains("foreach"));
        assertTrue("if should be paired", pairedMacros.contains("if"));

        long blockCount = pairedMacros.stream().filter(n -> n.equals("block")).count();
        long snippetCount = pairedMacros.stream().filter(n -> n.equals("snippet")).count();
        long foreachCount = pairedMacros.stream().filter(n -> n.equals("foreach")).count();
        long ifCount = pairedMacros.stream().filter(n -> n.equals("if")).count();

        assertEquals("Expected 3 paired {block} macros", 3, blockCount);
        assertEquals("Expected 3 paired {snippet} macros", 3, snippetCount);
        assertEquals("Expected 1 paired {foreach} macro", 1, foreachCount);
        assertEquals("Expected 2 paired {if} macros", 2, ifCount);
    }

    @Test
    public void testAutoClosedBlockParsesWithoutErrors() throws IOException {
        PsiFile file = parseFile("DeeplyNestedAutoClose", loadFile("DeeplyNestedAutoClose.latte"));

        List<String> errors = new ArrayList<>();
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PsiErrorElement) {
                    errors.add("at offset " + element.getTextOffset() + ": " + ((PsiErrorElement) element).getErrorDescription());
                }
                super.visitElement(element);
            }
        });

        String tree = toParseTreeText(file, true, false);
        assertTrue("PSI tree should have no errors but found: " + errors + "\nTree:\n" + tree, errors.isEmpty());
    }
}
