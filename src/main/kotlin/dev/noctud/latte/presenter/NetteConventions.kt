package dev.noctud.latte.presenter

import com.intellij.openapi.project.Project
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.latte.ext.allSubclasses
import dev.noctud.latte.ext.capitalized
import dev.noctud.latte.ext.decapitalized
import dev.noctud.latte.ext.isSubclassOf
import dev.noctud.latte.psi.LatteFile

/**
 * How Nette names things: which classes are presenters, which method a name in a link or a
 * `{control}` tag stands for, and which presenter a template belongs to. The role of a method is
 * in its name - `actionEdit` and `renderEdit` are the action `edit`, `handleDelete` the signal
 * `delete`, `createComponentMenu` the component `menu`.
 */
object NetteConventions {

    /** What a method is to a template. */
    enum class Role {
        /** An action of a presenter, `edit` in `{link edit}`. */
        ACTION,

        /** A signal of a presenter or a component, `delete` in `{link delete!}`. */
        SIGNAL,

        /** A component of a presenter or a component, `menu` in `{control menu}`. */
        COMPONENT,

        /** A way of rendering a component, `sidebar` in `{control menu:sidebar}`. */
        RENDER_MODE,
    }

    private const val PRESENTER = "\\Nette\\Application\\UI\\Presenter"

    /** A presenter method named like a signal that Nette calls itself. */
    private const val INVALID_LINK_HANDLER = "handleInvalidLink"

    private val prefixes = mapOf(
        Role.ACTION to listOf("action", "render"),
        Role.SIGNAL to listOf("handle"),
        Role.COMPONENT to listOf("createComponent"),
        Role.RENDER_MODE to listOf("render"),
    )

    fun isUsedIn(project: Project): Boolean {
        return basePresenter(project) != null
    }

    /** Every presenter of the project, the abstract ones included. */
    fun presenters(project: Project): List<PhpClass> {
        return basePresenter(project)?.allSubclasses() ?: emptyList()
    }

    fun isPresenter(phpClass: PhpClass): Boolean {
        return phpClass.isSubclassOf(PRESENTER)
    }

    /**
     * The name a link calls the presenter by, `Product` for `ProductPresenter`. An abstract
     * presenter stands for a module, so `BaseAdminPresenter` is `Admin`.
     */
    fun linkName(presenter: PhpClass): String {
        val name = presenter.name.removeSuffix("Presenter")
        if (!presenter.isAbstract) {
            return name
        }

        return if (name.startsWith("Abstract")) name.removePrefix("Abstract") else name.removePrefix("Base")
    }

    /** The name a template calls the method by in the role, or null when it does not play the role. */
    fun memberName(method: Method, role: Role): String? {
        val name = method.name
        if (role == Role.SIGNAL && name == INVALID_LINK_HANDLER) {
            return null
        }

        val prefix = prefixes.getValue(role).firstOrNull { name.startsWith(it) && name.length > it.length } ?: return null
        return name.removePrefix(prefix).decapitalized()
    }

    /**
     * The method a template means by the name in the role. An action is the action method when
     * there is one, and the render method otherwise.
     */
    fun findMember(phpClass: PhpClass, role: Role, name: String): Method? {
        for (prefix in prefixes.getValue(role)) {
            val method = phpClass.findMethodByName(prefix + name.capitalized())
            if (method != null && memberName(method, role) != null) {
                return method
            }
        }

        return null
    }

    /**
     * Names of the presenters the template may belong to, the likeliest first: the presenter of
     * `{templateType App\UI\Product\ProductTemplate}`, then the one named after the template's
     * directory, then after the template itself - `Product/edit.latte`, `Product-edit.latte` and
     * `Product.latte` are all `Product`.
     */
    fun templatePresenterNames(template: LatteFile): List<String> {
        val names = mutableListOf<String>()

        val templateType = template.firstLatteTemplateType?.types?.firstOrNull()
        if (templateType != null) {
            names.add(templateType.removeSuffix("Template") + "Presenter")
            names.add(templateType.substringAfterLast("\\").replace("Template", ""))
        }

        val file = template.originalFile
        val directoryName = file.containingDirectory?.name?.capitalized()
        if (directoryName != null && directoryName !in names) {
            names.add(directoryName)
        }

        val fileName = file.name.removeSuffix(".latte").capitalized()
        for (separator in listOf("-", "_")) {
            if (fileName.contains(separator)) {
                names.add(fileName.substringBefore(separator))
            }
        }

        if (fileName !in names) {
            names.add(fileName)
        }

        return names
    }

    /**
     * Names of the actions the template may be rendered by, the likeliest first: `edit.latte` and
     * `Product-edit.latte` are `edit`, and `default` is always a candidate.
     */
    fun templateActionNames(template: LatteFile): List<String> {
        val fileName = template.originalFile.name.removeSuffix(".latte")
        val names = mutableListOf<String>()

        for (separator in listOf("-", "_")) {
            val action = fileName.split(separator).getOrNull(1)
            if (!action.isNullOrEmpty()) {
                names.add(action)
            }
        }

        names.add(fileName.decapitalized())
        if ("default" !in names) {
            names.add("default")
        }

        return names
    }

    private fun basePresenter(project: Project): PhpClass? {
        return PhpIndex.getInstance(project).getAnyByFQN(PRESENTER).firstOrNull()
    }
}
