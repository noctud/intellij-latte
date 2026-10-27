package dev.noctud.latte.reference;

import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * Find usages from the PHP side: the templates that link to a presenter, an action or a signal,
 * or render a component.
 */
public class PresenterUsagesTest extends NetteProjectTestCase {

    private static final String PRODUCT = "\\App\\UI\\Product\\ProductPresenter";
    private static final String HOME = "\\App\\UI\\Home\\HomePresenter";
    private static final String DASHBOARD = "\\App\\UI\\Admin\\Dashboard\\DashboardPresenter";
    private static final String BASE = "\\App\\UI\\BasePresenter";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        template("app/UI/Product/default.latte", """
            <a n:href="edit, id: 1">edit</a>
            <a n:href="delete!, id: 1">delete</a>
            {link show, id: 1}
            {control productForm}
            {control menu:sidebar}
            """);
        template("app/UI/Product/edit.latte", """
            {link this}
            {form productForm}{/form}
            """);
        template("app/UI/Home/default.latte", """
            {link Product:edit, id: 1}
            {plink Product:default}
            {link logout!}
            {control menu}
            """);
        template("app/UI/Admin/Dashboard/default.latte", """
            {link :Admin:Dashboard:stats}
            {link :Home:default}
            """);
    }

    /** {@code this} in the action's own template counts. */
    public void testAnActionMethod() {
        assertEquals(
            List.of("app/UI/Home/default.latte: edit", "app/UI/Product/default.latte: edit", "app/UI/Product/edit.latte: this"),
            usagesOf(method(PRODUCT, "actionEdit"))
        );
    }

    /** A link goes to the action method when there is one, so the render method has no usages of its own. */
    public void testARenderMethodBehindAnActionMethod() {
        assertEquals(List.of(), usagesOf(method(PRODUCT, "renderEdit")));
    }

    public void testARenderMethod() {
        assertEquals(List.of("app/UI/Product/default.latte: show"), usagesOf(method(PRODUCT, "renderShow")));
        assertEquals(List.of("app/UI/Home/default.latte: default"), usagesOf(method(PRODUCT, "renderDefault")));
        assertEquals(List.of("app/UI/Admin/Dashboard/default.latte: default"), usagesOf(method(HOME, "renderDefault")));
        assertEquals(List.of("app/UI/Admin/Dashboard/default.latte: stats"), usagesOf(method(DASHBOARD, "renderStats")));
    }

    public void testASignal() {
        assertEquals(List.of("app/UI/Product/default.latte: delete!"), usagesOf(method(PRODUCT, "handleDelete")));
        assertEquals(List.of("app/UI/Home/default.latte: logout!"), usagesOf(method(BASE, "handleLogout")));
    }

    public void testAPresenter() {
        assertEquals(List.of("app/UI/Home/default.latte: Product", "app/UI/Home/default.latte: Product"), usagesOf(phpClass(PRODUCT)));
        assertEquals(List.of("app/UI/Admin/Dashboard/default.latte: Home"), usagesOf(phpClass(HOME)));
        assertEquals(List.of("app/UI/Admin/Dashboard/default.latte: Dashboard"), usagesOf(phpClass(DASHBOARD)));
    }

    /** A module is a usage of the abstract presenter it is named after. */
    public void testAModule() {
        assertEquals(List.of("app/UI/Admin/Dashboard/default.latte: Admin"), usagesOf(phpClass("\\App\\UI\\Admin\\AdminPresenter")));
    }

    public void testAComponentFactory() {
        assertEquals(
            List.of("app/UI/Product/default.latte: productForm", "app/UI/Product/edit.latte: productForm"),
            usagesOf(method(PRODUCT, "createComponentProductForm"))
        );
        assertEquals(
            List.of("app/UI/Home/default.latte: menu", "app/UI/Product/default.latte: menu"),
            usagesOf(method(BASE, "createComponentMenu"))
        );
    }

    public void testARenderMethodOfAComponent() {
        assertEquals(List.of("app/UI/Product/default.latte: sidebar"), usagesOf(method("\\App\\UI\\Menu\\MenuControl", "renderSidebar")));
    }

    /**
     * Renaming a method leaves the templates as they were: the references take part in the search
     * but refuse the new name. Should leave {@code {link detail, id: 1}} behind.
     */
    public void testRenamingAnActionLeavesTheTemplatesAlone() {
        myFixture.renameElement(method(PRODUCT, "renderShow"), "renderDetail");
        VirtualFile template = myFixture.getTempDirFixture().getFile("app/UI/Product/default.latte");
        assertTrue(FileDocumentManager.getInstance().getDocument(template).getText().contains("{link show, id: 1}"));
    }
}
