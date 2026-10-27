package dev.noctud.latte.presenter

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.latte.ext.allSubclasses
import dev.noctud.latte.presenter.NetteConventions.Role
import dev.noctud.latte.psi.LatteFile

/**
 * The presenters a template can reach, and what the names in its links and `{control}` tags
 * stand for.
 */
class TemplatePresenters private constructor(private val template: LatteFile) {

    private val project = template.project

    fun presenters(): List<PhpClass> {
        return NetteConventions.presenters(project)
    }

    /**
     * The presenter a link names, `Product` in `Product:edit`, with the presenters named before it
     * as context.
     */
    fun resolvePresenter(name: String, previousPresenters: List<String>, preferAbstract: Boolean): PhpClass? {
        return matchingPresenters(name, preferAbstract, previousPresenters).firstOrNull()
    }

    /** The presenter the template itself belongs to. */
    fun templatePresenter(previousPresenters: List<String> = emptyList()): PhpClass? {
        return matchingPresenters(null, false, previousPresenters).firstOrNull()
    }

    /** The method of an action, of the given presenter or else of the template's own. */
    fun resolveAction(presenter: PhpClass?, action: String): Method? {
        val actions = if (action == "this") NetteConventions.templateActionNames(template) else listOf(action)
        for (candidate in presenter?.let(::listOf) ?: matchingPresenters(null, false)) {
            for (name in actions) {
                NetteConventions.findMember(candidate, Role.ACTION, name)?.let { return it }
            }
        }

        return null
    }

    fun resolveSignal(presenter: PhpClass?, signal: String): Method? {
        for (candidate in presenter?.let(::listOf) ?: matchingPresenters(null, false)) {
            NetteConventions.findMember(candidate, Role.SIGNAL, signal)?.let { return it }
        }

        return null
    }

    /**
     * The factory of a component of the template's presenter. A template whose presenter cannot be
     * told searches every presenter, unless the name is too common to say anything.
     */
    fun resolveComponent(component: String): Method? {
        var candidates = matchingPresenters(null, false)
        if (candidates.isEmpty()) {
            if (component.lowercase() in COMMON_COMPONENT_NAMES) {
                return null
            }
            candidates = presenters()
        }

        for (candidate in candidates) {
            NetteConventions.findMember(candidate, Role.COMPONENT, component)?.let { return it }
        }

        return null
    }

    /** The components of the template's presenter, with their names. */
    fun components(): List<Pair<String, Method>> {
        val presenter = matchingPresenters(null, false).firstOrNull() ?: return emptyList()
        return members(presenter, Role.COMPONENT)
    }

    /** The methods of the class in the role, with the names a template uses for them. */
    fun members(phpClass: PhpClass, role: Role): List<Pair<String, Method>> {
        return phpClass.methods.mapNotNull { method -> NetteConventions.memberName(method, role)?.let { it to method } }
    }

    fun actionsForCompletion(presenter: PhpClass): List<LookupElement> {
        return members(presenter, Role.ACTION).map { (name, method) ->
            LookupElementBuilder.create(name).withTailText(" in " + ownerName(method, presenter)).withIcon(AllIcons.Actions.Execute)
        }
    }

    fun signalsForCompletion(presenter: PhpClass): List<LookupElement> {
        return members(presenter, Role.SIGNAL).map { (name, method) ->
            LookupElementBuilder.create("$name!").withTailText(" in " + ownerName(method, presenter)).withIcon(AllIcons.Actions.Lightning)
        }
    }

    /**
     * The presenters and modules that can follow `parent` in a link, or start one. A module is an
     * abstract presenter other presenters extend.
     */
    fun presentersForCompletion(parent: String?, fullyQualified: Boolean, root: Boolean): List<LookupElement> {
        val presenters = presenters()

        val parentClass = if (parent != null) {
            matchingPresenters(parent, true).firstOrNull()
        } else {
            matchingPresenters(null, true).firstOrNull()?.let { templatePresenter ->
                val superClass = templatePresenter.superClass
                when {
                    templatePresenter.isAbstract && templatePresenter in presenters -> templatePresenter
                    superClass != null && superClass in presenters -> superClass
                    else -> null
                }
            }
        }

        val atLeastOneChild = parentClass != null && parentClass.allSubclasses().size > 1
        val onlyBaseModules = fullyQualified && parent == null && presenters.any { it.isAbstract && NetteConventions.linkName(it).isNotEmpty() }

        val result = mutableListOf<LookupElement>()
        for (presenter in presenters) {
            if (parent != null && parentClass != null && presenter.isEquivalentTo(parentClass)) {
                continue
            }

            val name = NetteConventions.linkName(presenter)
            val superClass = presenter.superClass

            if ((onlyBaseModules || root) && presenter.isAbstract && name.isNotEmpty()) {
                if (superClass == null || superClass !in presenters || NetteConventions.linkName(superClass).isEmpty()) {
                    result.add(
                        LookupElementBuilder.create((if (root) ":" else "") + name + ":")
                            .withPresentableText(":$name")
                            .withTailText(": from " + presenter.name)
                            .withIcon(AllIcons.Actions.GroupByTestProduction)
                    )
                }
            }

            val isChildOfParent = superClass != null && parentClass != null && superClass.isEquivalentTo(parentClass)
            if (atLeastOneChild && isChildOfParent && name.isNotEmpty()) {
                result.add(presenterLookup(name, presenter))
            }

            if (!atLeastOneChild || root) {
                if (parentClass == null || isChildOfParent) {
                    result.add(presenterLookup(name, presenter))
                }
            }
        }

        return result
    }

    private fun presenterLookup(name: String, presenter: PhpClass): LookupElement {
        return LookupElementBuilder.create("$name:")
            .withPresentableText(name)
            .withTailText(": from " + presenter.name)
            .withIcon(if (presenter.isAbstract) AllIcons.Actions.GroupByTestProduction else AllIcons.Nodes.Property)
    }

    /**
     * The presenters a name can mean, the likeliest first, or the ones the template can belong to
     * when there is no name. Of several presenters of the same name, the one nearest the template
     * comes first.
     *
     * The presenters named before it in the link are meant to pick between presenters of the same
     * name in different modules. They do not yet (pinned by LinkResolveTest).
     */
    @Suppress("UNUSED_PARAMETER")
    private fun matchingPresenters(name: String?, preferAbstract: Boolean, previousPresenters: List<String> = emptyList()): List<PhpClass> {
        val names = if (name != null) listOf(name) else NetteConventions.templatePresenterNames(template)
        val presenters = presenters()
        val templatePath = template.originalFile.containingDirectory?.virtualFile?.path

        val candidates = LinkedHashMap<String, PhpClass>()
        for (presenterName in names) {
            for (presenter in presenters) {
                if (presenter.fqn == presenterName) {
                    return listOf(presenter)
                }

                if (NetteConventions.linkName(presenter) == presenterName) {
                    candidates.putIfAbsent(presenter.fqn, presenter)
                }
            }
        }

        val ordered = candidates.values.sortedBy { distance(templatePath, it) }
        if (preferAbstract) {
            val abstracts = ordered.filter { it.isAbstract }
            if (abstracts.isNotEmpty()) {
                return abstracts
            }
        }

        return ordered
    }

    /** How far apart the two directories are, counted in characters. */
    private fun distance(templatePath: String?, presenter: PhpClass): Int {
        val presenterPath = presenter.containingFile?.containingDirectory?.virtualFile?.path
        if (templatePath == null || presenterPath == null) {
            return Int.MAX_VALUE
        }

        val common = templatePath.commonPrefixWith(presenterPath).length
        return templatePath.length + presenterPath.length - 2 * common
    }

    private fun ownerName(method: Method, fallback: PhpClass): String {
        return method.containingClass?.name ?: fallback.name
    }

    companion object {
        private val COMMON_COMPONENT_NAMES = setOf("form", "grid", "datagrid", "addform", "editform")

        /** The presenters of the template's project, or null in a project that does not use Nette. */
        fun of(template: LatteFile): TemplatePresenters? {
            return if (NetteConventions.isUsedIn(template.project)) TemplatePresenters(template) else null
        }
    }
}
