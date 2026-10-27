package dev.noctud.latte.reference;

import java.util.List;

/**
 * What is offered while a component name is typed.
 */
public class ControlCompletionTest extends NetteProjectTestCase {

    private static final String PRODUCT_DEFAULT = "app/UI/Product/default.latte";

    /** The components of the template's presenter, inherited ones included. */
    public void testAComponent() {
        assertEquals(List.of("menu", "productForm"), complete(PRODUCT_DEFAULT, "{control <caret>}"));
    }

    public void testARenderMethod() {
        assertEquals(List.of("sidebar"), complete(PRODUCT_DEFAULT, "{control menu:<caret>}"));
    }

    /**
     * The subcomponents of the component. Nothing is offered, for the same reason a subcomponent
     * does not resolve (see {@link ControlResolveTest}); should be {@code items}.
     */
    public void testASubcomponentIsNotOffered() {
        assertEquals(List.of(), complete(PRODUCT_DEFAULT, "{control menu-<caret>}"));
    }

    public void testTheFormTags() {
        assertEquals(List.of("menu", "productForm"), complete(PRODUCT_DEFAULT, "{form <caret>}{/form}"));
        assertEquals(List.of("menu", "productForm"), complete(PRODUCT_DEFAULT, "<form n:name=\"<caret>\"></form>"));
    }

    public void testAComponentOfAPresenterInAModule() {
        assertEquals(List.of("chart", "menu"), complete("app/UI/Admin/Dashboard/default.latte", "{control <caret>}"));
    }
}
