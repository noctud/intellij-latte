package dev.noctud.latte.inspections;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.lang.psi.elements.Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code {=\VERSION}} prints a global constant. Latte 3.1 deprecates the unqualified {@code VERSION},
 * so the backslash is the way to write it, but the lexer gives every name starting with a backslash
 * to a class reference and the inspection reported it as {@code Undefined class '\VERSION'}.
 *
 * <p>The position tells a constant from a class, so a name in a class position is still checked as
 * a class even when a constant of that name exists.
 */
public class GlobalConstantTest extends BasePlatformTestCase {

    private static final String VERSION_PHP =
        "<?php\n"
            + "\n"
            + "define('VERSION', '6.1.1');\n";

    private static final String LIMIT_PHP =
        "<?php\n"
            + "\n"
            + "namespace App;\n"
            + "\n"
            + "const LIMIT = 5;\n";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("version.php", VERSION_PHP);
        myFixture.addFileToProject("app/limit.php", LIMIT_PHP);
        myFixture.enableInspections(new ClassUsagesInspection());
    }

    public void testAGlobalConstantIsNotAClass() {
        assertEquals(List.of(), problemsIn("{=\\VERSION}\n"));
        assertEquals(List.of(), problemsIn("<a href=\"{='adminer-' . \\VERSION . '.php'}\">\n"));
        assertEquals(List.of(), problemsIn("{if \\VERSION}{/if}\n"));
    }

    public void testANamespacedConstantIsNotAClassEither() {
        assertEquals(List.of(), problemsIn("{=\\App\\LIMIT}\n"));
    }

    /** A constant that is not there is still reported, as before. */
    public void testAMissingConstantIsReported() {
        assertEquals(List.of("WARNING:Undefined class '\\NO_SUCH'"), problemsIn("{=\\NO_SUCH}\n"));
    }

    /** {@code new}, {@code ::} and a type name a class, whatever constants exist. */
    public void testAClassPositionIsStillAClass() {
        assertEquals(List.of("WARNING:Undefined class '\\VERSION'"), problemsIn("{var $x = new \\VERSION}\n"));
        assertEquals(List.of("WARNING:Undefined class '\\VERSION'"), problemsIn("{=\\VERSION::class}\n"));
    }

    /**
     * A type is checked by its own rules, so the constant must not change what is reported there -
     * compared with a name that is neither a class nor a constant rather than with a fixed message.
     */
    public void testATypeIsNotAConstant() {
        List<String> missing = problemsIn("{varType \\NO_SUCH $x}\n");
        List<String> expected = new ArrayList<>();
        for (String problem : missing) {
            expected.add(problem.replace("NO_SUCH", "VERSION"));
        }
        assertEquals(expected, problemsIn("{varType \\VERSION $x}\n"));
    }

    public void testTheConstantIsTheTargetOfTheReference() {
        myFixture.configureByText("constant.latte", "{=\\VER<caret>SION}\n");
        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assertNotNull(reference);
        PsiElement target = reference.resolve();
        assertTrue(String.valueOf(target), target instanceof Constant);
        assertEquals("\\VERSION", ((Constant) target).getFQN());
    }

    private List<String> problemsIn(String template) {
        myFixture.configureByText("constant.latte", template);
        List<String> problems = new ArrayList<>();
        for (HighlightInfo info : myFixture.doHighlighting()) {
            if (info.getDescription() != null) {
                problems.add(info.getSeverity().getName() + ":" + info.getDescription());
            }
        }
        return problems;
    }
}
