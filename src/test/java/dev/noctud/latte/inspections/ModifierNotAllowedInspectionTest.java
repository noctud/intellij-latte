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

public class ModifierNotAllowedInspectionTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        URL url = getClass().getClassLoader().getResource("data/inspections/modifierNotAllowed");
        assert url != null;
        return url.getFile();
    }

    @Test
    public void testFilterNotAllowedInVar() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("FilterNotAllowedInVar.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Filters are not allowed here", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR, problems.get(0).getType());
    }

    @Test
    public void testFilterAllowedInBlock() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("FilterAllowedInBlock.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testNoFiltersUsed() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("NoFiltersUsed.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    private List<LatteInspectionInfo> getProblems(@NotNull String templateName) throws IOException {
        PsiFile file = parseFile(templateName);
        return (new ModifierNotAllowedInspection()).checkFile(file);
    }
}
