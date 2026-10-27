package dev.noctud.latte.reference;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usageView.UsageInfo;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import dev.noctud.latte.LatteLanguage;
import dev.noctud.latte.psi.LatteFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A small Nette application the link and control tests share, laid out the way Nette 3.2 lays one
 * out: each presenter in its own directory under {@code app/UI}, its templates next to it.
 *
 * <p>The tests pin what resolution, completion and find usages answer today, so that rewriting
 * them cannot change an answer without a test saying so. Where today's answer is wrong, the test
 * says that too, so a fix shows up as a deliberate change to an assertion rather than as noise.
 */
public abstract class NetteProjectTestCase extends BasePlatformTestCase {

    private static final String CARET = "<caret>";

    private static final String NETTE_PHP = """
        <?php
        namespace Nette\\ComponentModel;

        abstract class Component {}

        abstract class Container extends Component
        {
            protected function createComponent(string $name): ?Component {}
        }

        namespace Nette\\Application\\UI;

        abstract class Component extends \\Nette\\ComponentModel\\Container {}

        abstract class Control extends Component {}

        abstract class Presenter extends Control
        {
            protected function startup(): void {}

            protected function handleInvalidLink(\\Exception $e): string {}
        }

        class Form extends Component {}
        """;

    private static final String BASE_PRESENTER_PHP = """
        <?php
        namespace App\\UI;

        use App\\UI\\Menu\\MenuControl;
        use Nette\\Application\\UI\\Presenter;

        abstract class BasePresenter extends Presenter
        {
            public function handleLogout(): void {}

            protected function createComponentMenu(): MenuControl {}
        }
        """;

    private static final String MENU_CONTROL_PHP = """
        <?php
        namespace App\\UI\\Menu;

        use Nette\\Application\\UI\\Control;

        final class MenuControl extends Control
        {
            public function renderSidebar(): void {}

            public function handleToggle(): void {}

            protected function createComponentItems(): ItemsControl {}
        }

        final class ItemsControl extends Control
        {
            protected function createComponentBadge(): Control {}
        }
        """;

    private static final String HOME_PRESENTER_PHP = """
        <?php
        namespace App\\UI\\Home;

        use App\\UI\\BasePresenter;

        final class HomePresenter extends BasePresenter
        {
            public function renderDefault(): void {}
        }
        """;

    private static final String PRODUCT_PRESENTER_PHP = """
        <?php
        namespace App\\UI\\Product;

        use App\\UI\\BasePresenter;
        use Nette\\Application\\UI\\Form;

        final class ProductPresenter extends BasePresenter
        {
            public function actionEdit(int $id): void {}

            public function renderEdit(int $id): void {}

            public function renderDefault(): void {}

            public function renderShow(int $id): void {}

            public function actionArchive(): void {}

            public function handleDelete(int $id): void {}

            protected function createComponentProductForm(): Form {}
        }
        """;

    private static final String ADMIN_PRESENTER_PHP = """
        <?php
        namespace App\\UI\\Admin;

        use App\\UI\\BasePresenter;

        abstract class AdminPresenter extends BasePresenter {}
        """;

    private static final String ADMIN_DASHBOARD_PRESENTER_PHP = """
        <?php
        namespace App\\UI\\Admin\\Dashboard;

        use App\\UI\\Admin\\AdminPresenter;

        final class DashboardPresenter extends AdminPresenter
        {
            public function renderDefault(): void {}

            public function renderStats(): void {}

            protected function createComponentChart(): \\Nette\\Application\\UI\\Control {}
        }
        """;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("vendor/nette/nette.php", NETTE_PHP);
        myFixture.addFileToProject("app/UI/BasePresenter.php", BASE_PRESENTER_PHP);
        myFixture.addFileToProject("app/UI/Menu/MenuControl.php", MENU_CONTROL_PHP);
        myFixture.addFileToProject("app/UI/Home/HomePresenter.php", HOME_PRESENTER_PHP);
        myFixture.addFileToProject("app/UI/Product/ProductPresenter.php", PRODUCT_PRESENTER_PHP);
        myFixture.addFileToProject("app/UI/Admin/AdminPresenter.php", ADMIN_PRESENTER_PHP);
        myFixture.addFileToProject("app/UI/Admin/Dashboard/DashboardPresenter.php", ADMIN_DASHBOARD_PRESENTER_PHP);
    }

    /**
     * What the reference under {@code <caret>} resolves to, as {@code Class::method}, a class name,
     * {@code null} when it resolves to nothing, or {@code no reference} when there is none to ask.
     */
    protected @NotNull String resolve(@NotNull String path, @NotNull String template) {
        int caret = template.indexOf(CARET);
        assertTrue("no " + CARET + " in the template", caret >= 0);
        PsiFile file = template(path, template.replace(CARET, ""));
        PsiReference reference = file.getViewProvider().findReferenceAt(caret, LatteLanguage.INSTANCE);
        if (reference == null) {
            return "no reference";
        }
        return describe(reference.resolve());
    }

    /** The lookup strings offered at {@code <caret>}, or the text left behind when one was inserted. */
    protected @NotNull List<String> complete(@NotNull String path, @NotNull String template) {
        int caret = template.indexOf(CARET);
        assertTrue("no " + CARET + " in the template", caret >= 0);
        PsiFile file = template(path, template.replace(CARET, ""));
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
        myFixture.getEditor().getCaretModel().moveToOffset(caret);
        myFixture.completeBasic();
        List<String> strings = myFixture.getLookupElementStrings();
        if (strings == null) {
            return List.of("inserted: " + myFixture.getEditor().getDocument().getText());
        }
        return strings.stream().sorted().toList();
    }

    /**
     * Writes a template, over one already at the path if there is one. The file
     * {@link com.intellij.testFramework.fixtures.CodeInsightTestFixture#addFileToProject} returns
     * still has the empty content it was created with until the documents are committed.
     */
    protected @NotNull PsiFile template(@NotNull String path, @NotNull String text) {
        VirtualFile file = myFixture.getTempDirFixture().getFile(path);
        if (file == null) {
            file = myFixture.addFileToProject(path, text).getVirtualFile();
        } else {
            VirtualFile existing = file;
            try {
                WriteAction.run(() -> VfsUtil.saveText(existing, text));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
        assertNotNull(path, psiFile);
        return psiFile;
    }

    /**
     * Every usage of the element in a template, as {@code path: text of the referencing element}.
     * The usages PhpStorm finds in PHP itself are left out.
     */
    protected @NotNull List<String> usagesOf(@NotNull PsiElement element) {
        Collection<UsageInfo> usages = myFixture.findUsages(element);
        List<String> out = new ArrayList<>();
        for (UsageInfo usage : usages) {
            PsiElement usageElement = usage.getElement();
            PsiFile file = usage.getFile();
            if (usageElement == null || file == null || !(file.getViewProvider().getPsi(LatteLanguage.INSTANCE) instanceof LatteFile)) {
                continue;
            }
            String path = file.getVirtualFile().getPath().replaceFirst("^/src/", "");
            out.add(path + ": " + usageElement.getText());
        }
        out.sort(String::compareTo);
        return out;
    }

    protected @NotNull PhpClass phpClass(@NotNull String fqn) {
        Collection<PhpClass> classes = PhpIndex.getInstance(getProject()).getClassesByFQN(fqn);
        assertEquals("classes named " + fqn, 1, classes.size());
        return classes.iterator().next();
    }

    protected @NotNull Method method(@NotNull String fqn, @NotNull String name) {
        Method method = phpClass(fqn).findOwnMethodByName(name);
        assertNotNull(fqn + "::" + name, method);
        return method;
    }

    protected static @NotNull String describe(@Nullable PsiElement element) {
        if (element == null) {
            return "null";
        }
        if (element instanceof Method method) {
            PhpClass owner = method.getContainingClass();
            return (owner != null ? owner.getFQN() : "?") + "::" + method.getName();
        }
        if (element instanceof PhpClass phpClass) {
            return phpClass.getFQN();
        }
        return element.getClass().getSimpleName() + ":" + element.getText();
    }
}
