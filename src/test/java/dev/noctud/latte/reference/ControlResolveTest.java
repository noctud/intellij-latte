package dev.noctud.latte.reference;

/**
 * What a component name in {@code {control}}, {@code {form}} or {@code n:name} resolves to: the
 * factory method creating it, or a render method or factory of the component it creates.
 */
public class ControlResolveTest extends NetteProjectTestCase {

    private static final String PRODUCT_DEFAULT = "app/UI/Product/default.latte";

    public void testAComponentOfThePresenter() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::createComponentProductForm", resolve(PRODUCT_DEFAULT, "{control product<caret>Form}"));
    }

    public void testAnInheritedComponent() {
        assertEquals("\\App\\UI\\BasePresenter::createComponentMenu", resolve(PRODUCT_DEFAULT, "{control me<caret>nu}"));
    }

    public void testAComponentThatIsNotThere() {
        assertEquals("null", resolve(PRODUCT_DEFAULT, "{control noSu<caret>ch}"));
    }

    public void testArgumentsDoNotChangeTheComponent() {
        assertEquals("\\App\\UI\\BasePresenter::createComponentMenu", resolve(PRODUCT_DEFAULT, "{control me<caret>nu, 1, 2}"));
    }

    public void testARenderMethodOfTheComponent() {
        assertEquals("\\App\\UI\\Menu\\MenuControl::renderSidebar", resolve(PRODUCT_DEFAULT, "{control menu:side<caret>bar}"));
    }

    /** A render method that is not there points at the component's class instead. */
    public void testARenderMethodThatIsNotThere() {
        assertEquals("\\App\\UI\\Menu\\MenuControl", resolve(PRODUCT_DEFAULT, "{control menu:noSu<caret>ch}"));
    }

    public void testTheComponentBeforeARenderMethod() {
        assertEquals("\\App\\UI\\BasePresenter::createComponentMenu", resolve(PRODUCT_DEFAULT, "{control me<caret>nu:sidebar}"));
    }

    /** A subcomponent is created by a factory of the component before it. */
    public void testASubcomponent() {
        assertEquals("\\App\\UI\\Menu\\MenuControl::createComponentItems", resolve(PRODUCT_DEFAULT, "{control menu-it<caret>ems}"));
    }

    /** The first name of a hyphenated path is a component of the presenter, as it is on its own. */
    public void testTheFirstNameOfASubcomponentPath() {
        assertEquals("\\App\\UI\\BasePresenter::createComponentMenu", resolve(PRODUCT_DEFAULT, "{control me<caret>nu-items}"));
    }

    /** Each name is looked up on the class the factory before it returns, however long the path. */
    public void testASubcomponentTwoLevelsDown() {
        assertEquals("\\App\\UI\\Menu\\ItemsControl::createComponentBadge", resolve(PRODUCT_DEFAULT, "{control menu-items-bad<caret>ge}"));
        assertEquals("\\App\\UI\\Menu\\MenuControl::createComponentItems", resolve(PRODUCT_DEFAULT, "{control menu-it<caret>ems-badge}"));
    }

    /** A subcomponent nobody creates points at the class that would have to. */
    public void testASubcomponentThatIsNotThere() {
        assertEquals("\\App\\UI\\Menu\\MenuControl", resolve(PRODUCT_DEFAULT, "{control menu-noSu<caret>ch}"));
    }

    public void testARenderModeOfASubcomponent() {
        assertEquals("\\App\\UI\\Menu\\ItemsControl::renderCompact", resolve(PRODUCT_DEFAULT, "{control menu-items:comp<caret>act}"));
    }

    public void testTheFormTags() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::createComponentProductForm", resolve(PRODUCT_DEFAULT, "{form product<caret>Form}{/form}"));
        assertEquals("\\App\\UI\\Product\\ProductPresenter::createComponentProductForm", resolve(PRODUCT_DEFAULT, "<form n:name=\"product<caret>Form\"></form>"));
    }

    public void testAComponentOfAPresenterInAModule() {
        assertEquals(
            "\\App\\UI\\Admin\\Dashboard\\DashboardPresenter::createComponentChart",
            resolve("app/UI/Admin/Dashboard/default.latte", "{control ch<caret>art}")
        );
    }

    /**
     * A template whose presenter cannot be told: every presenter is searched, as a last resort,
     * except for a few names common enough to be a guess at best.
     */
    public void testATemplateWithoutAPresenterSearchesThemAll() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::createComponentProductForm", resolve("app/views/random.latte", "{control product<caret>Form}"));
        assertEquals("null", resolve("app/views/random.latte", "{control fo<caret>rm}"));
    }
}
