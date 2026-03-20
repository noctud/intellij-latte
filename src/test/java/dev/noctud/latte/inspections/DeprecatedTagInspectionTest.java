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

public class DeprecatedTagInspectionTest extends BasePsiParsingTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        URL url = getClass().getClassLoader().getResource("data/inspections/deprecatedTag");
        assert url != null;
        return url.getFile();
    }

    @Test
    public void testDeprecatedIfCurrent() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("DeprecatedIfCurrent.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(2, problems.size());

        Assert.assertEquals("Tag {ifCurrent} is deprecated in Latte 2.6. Use custom function isLinkCurrent() instead.", problems.get(0).getDescription());
        Assert.assertEquals(ProblemHighlightType.LIKE_DEPRECATED, problems.get(0).getType());

        // Close tag also produces a deprecation warning
        Assert.assertEquals(ProblemHighlightType.LIKE_DEPRECATED, problems.get(1).getType());
    }

    @Test
    public void testNoDeprecatedTags() throws IOException {
        List<LatteInspectionInfo> problems = getProblems("NoDeprecatedTags.latte");

        Assert.assertNotNull(problems);
        Assert.assertSame(0, problems.size());
    }

    private List<LatteInspectionInfo> getProblems(@NotNull String templateName) throws IOException {
        PsiFile file = parseFile(templateName);
        return (new DeprecatedTagInspection()).checkFile(file);
    }
}
