package dev.noctud.latte.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiFile;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.inspections.utils.LatteInspectionInfo;
import dev.noctud.latte.settings.LatteSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class VariablesInspectionTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // initialize configuration with test configuration
        LatteConfiguration.getInstance(getProject());

        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        URL url = getClass().getClassLoader().getResource("data/inspections/variables");
        assert url != null;
        return url.getFile();
    }

    @Test
    public void testUndefinedVariable() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("UndefinedVariable.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Undefined variable 'foo'", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR_OR_WARNING, problems.get(0).getType());
    }

    @Test
    public void testProbablyUndefinedVariable() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("ProbablyUndefinedVariable.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Variable 'bar' is probably undefined", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, problems.get(0).getType());
    }

    @Test
    public void testMultipleDefinitions() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("MultipleDefinitions.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(2, problems.size());

        Assert.assertEquals("Multiple definitions for variable 'foo'", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.WARNING, problems.get(0).getType());

        Assert.assertEquals("Multiple definitions for variable 'foo'", problems.get(1).getDescription());
        Assert.assertEquals(ProblemHighlightType.WARNING, problems.get(1).getType());
    }

    @Test
    public void testDefinitionsInAnotherContext() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("DefinitionsInAnotherContext.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Unused variable 'foo'", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.LIKE_UNUSED_SYMBOL, problems.get(0).getType());
    }

    @Test
    public void testVariableInNFor() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("VariableInNFor.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testVariableInBlock() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("VariableInBlock.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(2, problems.size());
        Assert.assertNotEquals(problems.get(0).getElement(), problems.get(1).getElement());

        Assert.assertEquals("Unused variable 'foo'", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.LIKE_UNUSED_SYMBOL, problems.get(0).getType());

        Assert.assertEquals("Unused variable 'foo'", problems.get(1).getDescription());
        Assert.assertEquals(ProblemHighlightType.LIKE_UNUSED_SYMBOL, problems.get(1).getType());
    }

    @Test
    public void testVariableDefinedInParentScope() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInParentScope.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testVariableDefinedInParentScopeNestedIf() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInParentScope2.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testArrowFunctionParameter() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("ArrowFunctionParameter.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testVariableDefinedInIfElse() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInIfElse.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testVariableDefinedInIfOnly() throws IOException {
        // Variable defined only in {if} (no {else}) — should warn as probably undefined
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInIfOnly.latte");

        Assert.assertNotNull(problems);
        LatteInspectionInfo probablyUndefined = problems.stream()
            .filter(p -> p.getDescription().contains("probably undefined"))
            .findFirst().orElse(null);
        Assert.assertNotNull("Expected 'probably undefined' warning", probablyUndefined);
    }

    @Test
    public void testVariableDefinedInElseifNoElse() throws IOException {
        // Variable defined in {if} and {elseif} but no final {else} — should warn
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInElseifNoElse.latte");

        Assert.assertNotNull(problems);
        LatteInspectionInfo probablyUndefined = problems.stream()
            .filter(p -> p.getDescription().contains("probably undefined"))
            .findFirst().orElse(null);
        Assert.assertNotNull("Expected 'probably undefined' warning without final {else}", probablyUndefined);
    }

    @Test
    public void testVariableDefinedInAllElseifBranches() throws IOException {
        // Variable defined in {if}, {elseif}, and {else} — all branches covered, no warning
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInAllElseifBranches.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testVariableDefinedInIfCondition() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("VariableDefinedInIfCondition.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testVariableAssignedInConditionScope() throws IOException {
        // {if $a = getSomething()} defines $a, but using $a after {/if} should be "probably undefined"
        List<LatteInspectionInfo> problems = getProblems("VariableAssignedInConditionScope.latte");

        Assert.assertNotNull(problems);
        LatteInspectionInfo probablyUndefined = problems.stream()
            .filter(p -> p.getDescription().contains("probably undefined"))
            .findFirst().orElse(null);
        Assert.assertNotNull("Expected 'probably undefined' for variable used after conditional assignment", probablyUndefined);
    }

    @Test
    public void testComparisonNotDefinition() throws IOException {
        // {if $a == 'test'} is a comparison, not a definition — $a should be undefined
        List<LatteInspectionInfo> problems = getProblems("ComparisonNotDefinition.latte");

        Assert.assertNotNull(problems);
        LatteInspectionInfo undefined = problems.stream()
            .filter(p -> p.getDescription().contains("ndefined variable 'a'"))
            .findFirst().orElse(null);
        Assert.assertNotNull("Expected undefined variable warning for comparison-only usage", undefined);
    }

    @Test
    public void testClosureParameter() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("ClosureParameter.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    private List<LatteInspectionInfo> getProblems(@NotNull String templateName) throws IOException {
        PsiFile file = parseFile(templateName);

        return (new VariablesInspection()).checkFile(file);
    }

}
