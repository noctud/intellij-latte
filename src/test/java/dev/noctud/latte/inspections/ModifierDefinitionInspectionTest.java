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

public class ModifierDefinitionInspectionTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        URL url = getClass().getClassLoader().getResource("data/inspections/modifierDefinition");
        assert url != null;
        return url.getFile();
    }

    @Test
    public void testUndefinedFilter() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("UndefinedFilter.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Undefined latte filter 'nonExistentFilter'", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR_OR_WARNING, problems.get(0).getType());
    }

    @Test
    public void testDefinedFilter() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("DefinedFilter.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    @Test
    public void testMultipleUndefinedFilters() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("MultipleUndefinedFilters.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(2, problems.size());

        Assert.assertEquals("Undefined latte filter 'unknownOne'", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR_OR_WARNING, problems.get(0).getType());

        Assert.assertEquals("Undefined latte filter 'unknownTwo'", problems.get(1).getDescription());
        Assert.assertEquals(ProblemHighlightType.GENERIC_ERROR_OR_WARNING, problems.get(1).getType());
    }

    @Test
    public void testMissingFilterParams() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("MissingFilterParams.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(1, problems.size());

        Assert.assertEquals("Missing required filter parameters (1 required)", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.WARNING, problems.get(0).getType());
    }

    /**
     * The value a filter is applied to is not one of its arguments. {@code batch} was registered
     * as {@code ($array, $length [, $item])}, the shape of the PHP function behind it, so
     * {@code |batch:2} was reported as missing a parameter it cannot be given: the array is what
     * stands to the left of the pipe.
     */
    @Test
    public void testFilterAppliedToItsSubject() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("FilterAppliedToItsSubject.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    private List<LatteInspectionInfo> getProblems(@NotNull String templateName) throws IOException {
        PsiFile file = parseFile(templateName);
        return (new ModifierDefinitionInspection()).checkFile(file);
    }
}
