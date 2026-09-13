package dev.noctud.latte.parser;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.psi.LattePhpArrayItem;
import dev.noctud.latte.settings.LatteSettings;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * An array item parses its key and its value as two expressions of the same kind; the item still
 * tells them apart.
 */
public class ArrayItemKeyValueTest extends BasePsiParsingTestCase {

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

    @Test
    public void testAKeyIsTheFirstOfTwoExpressions() {
        List<LattePhpArrayItem> items = itemsIn("{= ['x' => 1, 2]}");

        Assert.assertEquals(2, items.size());
        Assert.assertNotNull(items.get(0).getKey());
        Assert.assertEquals("'x'", items.get(0).getKey().getText().trim());
        Assert.assertEquals("1", items.get(0).getValue().getText().trim());

        Assert.assertNull("an item without an arrow has no key", items.get(1).getKey());
        Assert.assertEquals("2", items.get(1).getValue().getText().trim());
    }

    private List<LattePhpArrayItem> itemsIn(String template) {
        PsiFile file = parseFile("ArrayItem.latte", template);
        return new ArrayList<>(PsiTreeUtil.findChildrenOfType(file, LattePhpArrayItem.class));
    }
}
