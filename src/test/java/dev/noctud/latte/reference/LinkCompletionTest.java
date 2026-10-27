package dev.noctud.latte.reference;

import java.util.List;

/**
 * What is offered while a link destination is typed.
 */
public class LinkCompletionTest extends NetteProjectTestCase {

    private static final String PRODUCT_DEFAULT = "app/UI/Product/default.latte";

    private static final List<String> FROM_PRODUCT = List.of(
        ":Admin:", "Admin:", "Home:", "Product:",
        "archive", "default", "delete!", "edit", "logout!", "show", "this"
    );

    /** The presenters, and the actions and signals of the template's own presenter. */
    public void testAnEmptyDestination() {
        assertEquals(FROM_PRODUCT, complete(PRODUCT_DEFAULT, "{link <caret>}"));
    }

    /** A lower-case start is an action or a signal, never a presenter. */
    public void testAnAction() {
        assertEquals(List.of("archive", "default", "delete!", "edit"), complete(PRODUCT_DEFAULT, "{link e<caret>}"));
    }

    public void testAPresenter() {
        assertEquals(List.of("inserted: {link Home:}"), complete(PRODUCT_DEFAULT, "{link H<caret>}"));
    }

    /** The actions of the presenter named, and no presenters, since none can follow one. */
    public void testAnActionOfAnotherPresenter() {
        assertEquals(List.of("default"), complete(PRODUCT_DEFAULT, "{link Home:<caret>}"));
    }

    public void testAnAbsoluteDestination() {
        assertEquals(List.of("Admin:", "Home:", "Product:"), complete(PRODUCT_DEFAULT, "{link :<caret>}"));
    }

    /** After a module, the presenters of the module. */
    public void testAPresenterInAModule() {
        assertEquals(List.of("Dashboard:"), complete(PRODUCT_DEFAULT, "{link :Admin:<caret>}"));
    }

    public void testAnActionOfAPresenterInAModule() {
        assertEquals(List.of("default", "stats"), complete(PRODUCT_DEFAULT, "{link :Admin:Dashboard:<caret>}"));
    }

    /** Inside a module, the presenters offered are the module's own. */
    public void testFromATemplateOfAPresenterInAModule() {
        assertEquals(
            List.of(":Admin:", "Dashboard:", "default", "logout!", "stats", "this"),
            complete("app/UI/Admin/Dashboard/default.latte", "{link <caret>}")
        );
    }

    public void testTheOtherLinkTags() {
        assertEquals(FROM_PRODUCT, complete(PRODUCT_DEFAULT, "<a n:href=\"<caret>\">x</a>"));
        assertEquals(FROM_PRODUCT, complete(PRODUCT_DEFAULT, "{plink <caret>}"));
    }
}
