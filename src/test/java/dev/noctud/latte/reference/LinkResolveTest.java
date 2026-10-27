package dev.noctud.latte.reference;

/**
 * What a link destination in a template resolves to: a presenter, an action or render method, or
 * a signal handler.
 */
public class LinkResolveTest extends NetteProjectTestCase {

    private static final String PRODUCT_DEFAULT = "app/UI/Product/default.latte";

    public void testAnActionPrefersTheActionMethodToTheRenderMethod() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve(PRODUCT_DEFAULT, "{link ed<caret>it}"));
    }

    public void testAnActionWithOnlyARenderMethod() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::renderShow", resolve(PRODUCT_DEFAULT, "{link sh<caret>ow}"));
    }

    public void testAnActionWithOnlyAnActionMethod() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionArchive", resolve(PRODUCT_DEFAULT, "{link arch<caret>ive}"));
    }

    public void testAnActionThatIsNotThere() {
        assertEquals("null", resolve(PRODUCT_DEFAULT, "{link noSu<caret>ch}"));
    }

    public void testArgumentsDoNotChangeTheDestination() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::renderShow", resolve(PRODUCT_DEFAULT, "{link sh<caret>ow, id: 1}"));
        assertEquals("\\App\\UI\\Product\\ProductPresenter::renderShow", resolve(PRODUCT_DEFAULT, "{link sh<caret>ow 1}"));
    }

    public void testThisIsTheActionOfTheTemplate() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve("app/UI/Product/edit.latte", "{link th<caret>is}"));
        assertEquals("\\App\\UI\\Product\\ProductPresenter::renderShow", resolve("app/UI/Product/show.latte", "{link th<caret>is}"));
    }

    /** A template with no action method of its own: {@code this} falls back to the default action. */
    public void testThisInATemplateWithoutAnActionMethod() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::renderDefault", resolve("app/UI/Product/list.latte", "{link th<caret>is}"));
    }

    public void testAnAbsoluteUrlLink() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve(PRODUCT_DEFAULT, "{link //ed<caret>it}"));
    }

    public void testAFragment() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve(PRODUCT_DEFAULT, "{link ed<caret>it#top}"));
    }

    public void testAPresenterWithItsDefaultAction() {
        assertEquals("\\App\\UI\\Home\\HomePresenter", resolve(PRODUCT_DEFAULT, "{link Ho<caret>me:}"));
    }

    public void testAnAbsoluteLinkWithoutAModule() {
        assertEquals("\\App\\UI\\Home\\HomePresenter", resolve(PRODUCT_DEFAULT, "{link :Ho<caret>me:default}"));
        assertEquals("\\App\\UI\\Home\\HomePresenter::renderDefault", resolve(PRODUCT_DEFAULT, "{link :Home:def<caret>ault}"));
    }

    public void testASignal() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::handleDelete", resolve(PRODUCT_DEFAULT, "{link del<caret>ete!}"));
    }

    public void testAnInheritedSignal() {
        assertEquals("\\App\\UI\\BasePresenter::handleLogout", resolve(PRODUCT_DEFAULT, "{link log<caret>out!}"));
    }

    public void testASignalThatIsNotThere() {
        assertEquals("null", resolve(PRODUCT_DEFAULT, "{link noSu<caret>ch!}"));
    }

    public void testAnotherPresenter() {
        assertEquals("\\App\\UI\\Home\\HomePresenter", resolve(PRODUCT_DEFAULT, "{link Ho<caret>me:default}"));
    }

    public void testAnActionOfAnotherPresenter() {
        assertEquals("\\App\\UI\\Home\\HomePresenter::renderDefault", resolve(PRODUCT_DEFAULT, "{link Home:def<caret>ault}"));
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve("app/UI/Home/default.latte", "{link Product:ed<caret>it}"));
    }

    public void testAnAbsoluteModuleLink() {
        String link = "{link :Admin:Dashboard:stats}";
        assertEquals("\\App\\UI\\Admin\\AdminPresenter", resolve(PRODUCT_DEFAULT, link.replace("Admin:", "Adm<caret>in:")));
        assertEquals("\\App\\UI\\Admin\\Dashboard\\DashboardPresenter", resolve(PRODUCT_DEFAULT, link.replace("Dashboard", "Dash<caret>board")));
        assertEquals("\\App\\UI\\Admin\\Dashboard\\DashboardPresenter::renderStats", resolve(PRODUCT_DEFAULT, link.replace("stats", "st<caret>ats")));
    }

    public void testTheOtherLinkTags() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve(PRODUCT_DEFAULT, "{plink ed<caret>it}"));
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve(PRODUCT_DEFAULT, "<a n:href=\"ed<caret>it\">x</a>"));
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve(PRODUCT_DEFAULT, "<a n:href=\"ed<caret>it, id: 1\">x</a>"));
    }

    /** The presenter of a template comes from its directory, so the old {@code templates/<Presenter>/} layout works too. */
    public void testTheOldTemplatesDirectoryLayout() {
        assertEquals("\\App\\UI\\Product\\ProductPresenter::actionEdit", resolve("app/Presenters/templates/Product/default.latte", "{link ed<caret>it}"));
    }

    /**
     * {@code <Presenter>.<action>.latte}, the flat layout Nette also accepts. Not recognised: the
     * presenter is guessed from the file name split at {@code -} or {@code _}, never at a dot.
     * Should resolve to {@code ProductPresenter::actionEdit}.
     */
    public void testTheFlatTemplateLayoutIsNotRecognised() {
        assertEquals("null", resolve("app/Presenters/templates/Product.default.latte", "{link ed<caret>it}"));
    }

    /** {@code {templateType}} names the presenter when the template is somewhere its path says nothing. */
    public void testTheTemplateTypeNamesThePresenter() {
        assertEquals(
            "\\App\\UI\\Product\\ProductPresenter::actionEdit",
            resolve("app/views/whatever.latte", "{templateType App\\UI\\Product\\ProductTemplate}\n{link ed<caret>it}")
        );
    }

    /**
     * A signal of a component is addressed through the component. Not supported: the whole
     * {@code menu-toggle!} is looked up as a signal of the presenter. Should resolve to
     * {@code MenuControl::handleToggle}.
     */
    public void testASignalOfAComponentIsNotResolved() {
        assertEquals("null", resolve(PRODUCT_DEFAULT, "{link menu-tog<caret>gle!}"));
    }

    /**
     * Two presenters of the same name in different modules: the module in the link should pick
     * one. It does not - the module is meant to score the candidates by namespace, but the
     * namespace is matched against a doubled backslash, which never occurs, so the presenter
     * nearer the template wins. Should resolve to the Admin {@code DashboardPresenter::renderStats}.
     */
    public void testTheModuleDoesNotDecideBetweenTwoPresentersOfTheSameName() {
        myFixture.addFileToProject("app/UI/Front/Dashboard/DashboardPresenter.php", """
            <?php
            namespace App\\UI\\Front\\Dashboard;

            final class DashboardPresenter extends \\App\\UI\\BasePresenter
            {
                public function renderDefault(): void {}
            }
            """);

        assertEquals("null", resolve("app/UI/Front/Dashboard/default.latte", "{link :Admin:Dashboard:st<caret>ats}"));
        assertEquals(
            "\\App\\UI\\Front\\Dashboard\\DashboardPresenter",
            resolve("app/UI/Front/Dashboard/default.latte", "{link :Admin:Dash<caret>board:stats}")
        );
    }
}
