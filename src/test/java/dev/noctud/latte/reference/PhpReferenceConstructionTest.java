package dev.noctud.latte.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.php.LattePhpUtil;
import dev.noctud.latte.psi.LattePhpClassUsage;
import dev.noctud.latte.psi.LattePhpConstant;
import dev.noctud.latte.psi.LattePhpMethod;
import dev.noctud.latte.psi.LattePhpProperty;
import dev.noctud.latte.psi.LattePhpStaticVariable;
import dev.noctud.latte.reference.references.LattePhpConstantReference;
import dev.noctud.latte.reference.references.LattePhpMethodReference;
import dev.noctud.latte.reference.references.LattePhpPropertyReference;
import dev.noctud.latte.reference.references.LattePhpStaticVariableReference;
import dev.noctud.latte.settings.LatteSettings;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * References must be cheap to construct. The platform builds them from
 * {@code PsiReferenceContributor} at moments when the PHP stubs and the file text may still
 * disagree; reading the PHP index there is answered with a
 * {@code StubTextInconsistencyException}, which is what crashed the plugin in production.
 *
 * The mock project of this test has no {@code PhpIndex} service at all, so any lookup fails here
 * loudly instead of intermittently in the IDE. Construction therefore has to stay silent and
 * {@code multiResolve()} - where the lookup belongs - has to reach the index.
 */
public class PhpReferenceConstructionTest extends BasePsiParsingTestCase {

    private static final String SOURCE =
        "{varType \\App\\Model\\Article $article}\n"
            + "{$article->title}\n"
            + "{$article->getTitle()}\n"
            + "{$article::TITLE}\n"
            + "{$article::$instances}\n";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        return ".";
    }

    @Test
    public void testPropertyReference() {
        assertLookupHappensOnResolve(LattePhpProperty.class, LattePhpPropertyReference::new);
    }

    @Test
    public void testMethodReference() {
        assertLookupHappensOnResolve(LattePhpMethod.class, LattePhpMethodReference::new);
    }

    @Test
    public void testConstantReference() {
        assertLookupHappensOnResolve(LattePhpConstant.class, LattePhpConstantReference::new);
    }

    @Test
    public void testStaticVariableReference() {
        assertLookupHappensOnResolve(LattePhpStaticVariable.class, LattePhpStaticVariableReference::new);
    }

    @Test
    public void testClassReference() {
        assertLookupHappensOnResolve(
            LattePhpClassUsage.class,
            dev.noctud.latte.reference.references.LattePhpClassReference::new
        );
    }

    private <T extends PsiElement> void assertLookupHappensOnResolve(
        Class<T> elementType,
        ReferenceFactory<T> factory
    ) {
        PsiFile file = createPsiFile("typed.latte", SOURCE);
        ensureParsed(file);

        List<T> elements = new ArrayList<>(PsiTreeUtil.collectElementsOfType(file, elementType));
        Assert.assertFalse("no " + elementType.getSimpleName() + " parsed from the test source", elements.isEmpty());

        for (T element : elements) {
            TextRange range = new TextRange(0, element.getTextLength());

            PsiPolyVariantReference reference;
            try {
                reference = factory.create(element, range);
            } catch (Throwable t) {
                throw new AssertionError("constructing a reference for '" + element.getText()
                    + "' read the PHP index", t);
            }

            Assert.assertTrue(
                "'" + element.getText() + "' resolved without reading the PHP index - the lookup"
                    + " was dropped instead of deferred",
                readsPhpIndex(() -> reference.multiResolve(false))
            );
        }
    }

    /**
     * The lookup is recognised by the frame it fails in, not by what the failure says: the mock
     * project has no {@code PhpIndex}, so whatever is thrown carries our own call to it in its
     * stack. Matching the message instead would tie the test to wording nobody here owns.
     */
    private boolean readsPhpIndex(Runnable action) {
        try {
            action.run();
            return false;
        } catch (Throwable t) {
            for (StackTraceElement frame : t.getStackTrace()) {
                if (LattePhpUtil.class.getName().equals(frame.getClassName())) {
                    return true;
                }
            }
            return false;
        }
    }

    @FunctionalInterface
    private interface ReferenceFactory<T extends PsiElement> {
        PsiPolyVariantReference create(T element, TextRange range);
    }
}
