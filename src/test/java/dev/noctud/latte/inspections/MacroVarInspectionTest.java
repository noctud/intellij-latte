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

public class MacroVarInspectionTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        URL url = getClass().getClassLoader().getResource("data/inspections/macroVar");
        assert url != null;
        return url.getFile();
    }

    @Test
    public void testValidVarDefinition() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("ValidVarDefinition.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testMissingDefinitionOperator() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("MissingDefinitionOperator.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Tag {var} must contain definition operator (=).", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR, problems.get(0).getType());
    }

    @Test
    public void testMissingContentAfterEquals() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("MissingContentAfterEquals.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Tag {var} must contain variable content after =.", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR, problems.get(0).getType());
    }

    @Test
    public void testTypedVarDefinition() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("TypedVarDefinition.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    private List<LatteInspectionInfo> getProblems(@NotNull String templateName) throws IOException {
        PsiFile file = parseFile(templateName);
        return (new MacroVarInspection()).checkFile(file);
    }
}
