package dev.noctud.latte.php;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.noctud.latte.LatteLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code {var $a}} declares the name, so nothing is undefined about it.
 *
 * <p>Latte lets a {@code {var}} tag declare a name without giving it a value - both ends of the
 * supported range compile it, pass {@code php -l} on the generated code and render it, with the
 * name set to null. The plugin knew a variable was defined only when an {@code =} followed it, so
 * every such declaration and every use of it was reported as undefined.
 *
 * <p>It stayed out of sight because a second, louder report sat on top of it: the same tag was
 * called a missing assignment, and an error covering the whole tag hides the warnings inside it.
 * Removing the wrong error uncovered 42 wrong warnings over a corpus of real templates, which is
 * why the two belong to one change.
 */
public class VarDeclarationWithoutValueTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        List<LocalInspectionTool> tools = new ArrayList<>();
        for (LocalInspectionEP ep : LocalInspectionEP.LOCAL_INSPECTION.getExtensionList()) {
            if (LatteLanguage.INSTANCE.getID().equals(ep.language)) {
                tools.add((LocalInspectionTool) ep.instantiateTool());
            }
        }
        assertFalse("the plugin registers no Latte inspection at all", tools.isEmpty());
        myFixture.enableInspections(tools.toArray(new LocalInspectionTool[0]));
    }

    public void testADeclaredNameIsDefined() {
        assertEquals(List.of(), problemsIn("{var $a}\n{$a}\n"));
    }

    public void testEveryNameInAListIsDefined() {
        assertEquals(List.of(), problemsIn("{var $a, $b}\n{$a}{$b}\n"));
    }

    public void testATypedDeclarationDefinesItsNameToo() {
        assertEquals(List.of(), problemsIn("{var string $a}\n{$a}\n"));
    }

    public void testADeclarationMixedWithAnAssignment() {
        assertEquals(List.of(), problemsIn("{var $a = 1, $b}\n{$a}{$b}\n"));
    }

    /**
     * The counterweight, and the reason the rule cannot be "any variable inside a {var} tag".
     * What stands on the right of an {@code =} is read, not declared, and has to stay reported.
     */
    public void testWhatIsReadOnTheRightOfAnAssignmentIsNotDeclared() {
        assertEquals(
            List.of("WARNING:Undefined variable 'b'"),
            problemsIn("{var $a = $b}\n{$a}\n")
        );
        assertEquals(
            List.of("WARNING:Undefined variable 'd'"),
            problemsIn("{var $a = 1, $c = $d}\n{$a}{$c}\n")
        );
    }

    /** And an assignment that was already understood must go on being understood. */
    public void testAnOrdinaryAssignmentIsUnchanged() {
        assertEquals(List.of(), problemsIn("{var $a = 1}\n{$a}\n"));
    }

    public void testATypedListDefinesEveryName() {
        assertEquals(List.of(), problemsIn("{var string $a, int $b}\n{$a}{$b}\n"));
    }

    /** A declaration is a forward declaration, not a competing assignment. */
    public void testDeclaringFirstAndFillingLaterIsNotAMultipleDefinition() {
        assertEquals(List.of(), problemsIn("{var $a}\n{var $a = 1}\n{$a}\n"));
        assertEquals(List.of(), problemsIn("{var $a, $b}\n{var $a = 1}\n{var $b = 2}\n{$a}{$b}\n"));
    }

    /** Two real assignments in one context are still reported. */
    public void testTwoAssignmentsAreStillAMultipleDefinition() {
        assertTrue(problemsIn("{var $a = 1}\n{var $a = 2}\n{$a}\n").contains("WARNING:Multiple definitions for variable 'a'"));
    }

    public void testADefaultTagDeclaresToo() {
        assertEquals(List.of(), problemsIn("{default $a}\n{$a}\n"));
    }

    /** {@code $x, $c} parses into one node, so the comma sits inside it. */
    public void testANameAfterAVariableOnTheRightIsDeclared() {
        assertEquals(List.of(), problemsIn("{var $x = 1}\n{var $a = $x, $c}\n{$a}{$c}\n"));
    }

    private List<String> problemsIn(String template) {
        myFixture.configureByText("var-declaration.latte", template);
        List<String> problems = new ArrayList<>();
        for (HighlightInfo info : myFixture.doHighlighting()) {
            if (info.getDescription() != null) {
                problems.add(info.getSeverity().getName() + ":" + info.getDescription());
            }
        }
        return problems;
    }
}
