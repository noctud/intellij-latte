package dev.noctud.latte.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.settings.LatteSettings;
import org.junit.Assert;
import org.junit.Test;

/**
 * The arrow guard keeps one walk per tag and answers the rest of that tag's items from it, so the
 * shapes that matter are the ones where the answer differs either side of a boundary.
 */
public class ArrowCacheTest extends BasePsiParsingTestCase {

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

    private String treeOf(String template) {
        PsiFile file = parseFile("ArrowCache.latte", template);
        return DebugUtil.psiToString(file, true, false);
    }

    private void assertKeyed(String template, int howMany) {
        String tree = treeOf(template);
        int found = tree.split("PHP_KEY_ARRAY_ITEM", -1).length - 1;
        Assert.assertEquals(template + "\n" + tree, howMany, found);
    }

    @Test
    public void testATagWithAnArrowDoesNotLendItToTheNextOne() {
        assertKeyed("{link a, x => 1}{link b, y}", 1);
        assertKeyed("{link a, y}{link b, x => 1}", 1);
        assertKeyed("{link a, x => 1}{link b, y => 2}", 2);
        assertKeyed("{link a, y}{link b, z}", 0);
    }

    @Test
    public void testAnArrowInOneAttributeDoesNotReachTheNext() {
        assertKeyed("<a n:href=\"a, x => 1\" title=\"t\">x</a>", 1);
        assertKeyed("<a n:href=\"a, y\" n:class=\"b, x => 1\">x</a>", 1);
    }

    @Test
    public void testAnArrowInsideANestedArrayStillCountsForTheTag() {
        Assert.assertTrue(treeOf("{var $a = ['x' => 1]}").contains("PHP_ARRAY_ITEM"));
        assertKeyed("{link a, k => ['x' => 1]}", 1);
    }

    @Test
    public void testTheSameTagTextTwiceParsesTheSameWay() {
        Assert.assertEquals(
            treeOf("{link a, x => 1}").replace("ArrowCache", ""),
            treeOf("{link a, x => 1}").replace("ArrowCache", ""));
    }
}
