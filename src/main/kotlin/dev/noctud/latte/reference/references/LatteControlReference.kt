package dev.noctud.latte.reference.references

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.latte.presenter.NetteConventions
import dev.noctud.latte.presenter.NetteConventions.Role
import dev.noctud.latte.presenter.TemplatePresenters
import dev.noctud.latte.psi.LatteFile

/**
 * One name in a `{control}` path: a component of the template's presenter (`menu`), a component
 * of another component (`items` in `menu-items`), or a render mode (`sidebar` in `menu:sidebar`).
 */
class LatteControlReference(
    element: PsiElement,
    rangeInElement: TextRange,
    soft: Boolean,
    private val text: String,
    private val kind: Kind,
    /** The component that creates a subcomponent, or that a render mode renders. */
    private val owner: String?,
) : PsiReferenceBase<PsiElement>(element, rangeInElement, soft) {

    enum class Kind { COMPONENT, SUBCOMPONENT, RENDER_MODE }

    override fun resolve(): PsiElement? {
        val presenters = templatePresenters() ?: return null
        return when (kind) {
            Kind.COMPONENT -> presenters.resolveComponent(text)
            Kind.SUBCOMPONENT -> ownerClass(presenters)?.let { NetteConventions.findMember(it, Role.COMPONENT, text) ?: it }
            Kind.RENDER_MODE -> ownerClass(presenters)?.let { NetteConventions.findMember(it, Role.RENDER_MODE, text) ?: it }
        }
    }

    override fun getVariants(): Array<Any> {
        val presenters = templatePresenters() ?: return emptyArray()
        return when (kind) {
            Kind.COMPONENT -> presenters.components().mapNotNull { (name, method) ->
                method.containingClass?.let { LookupElementBuilder.create(name).withTailText(" from " + it.name).withIcon(AllIcons.Actions.GroupByModule) }
            }

            Kind.SUBCOMPONENT -> ownerMembers(presenters, Role.COMPONENT)
            Kind.RENDER_MODE -> ownerMembers(presenters, Role.RENDER_MODE)
        }.toTypedArray()
    }

    /** False, so renaming a PHP class or method does not try to rename the component. */
    override fun isReferenceTo(element: PsiElement): Boolean {
        return false
    }

    /**
     * What [isReferenceTo] would say if it did not refuse, for find usages. The names are compared
     * first so that most tags are turned away without resolving them.
     */
    fun isRefTo(element: PsiElement): Boolean {
        if (element is Method) {
            val role = if (kind == Kind.RENDER_MODE) Role.RENDER_MODE else Role.COMPONENT
            if (!NetteConventions.memberName(element, role).equals(text, ignoreCase = true)) {
                return false
            }
        }

        return super.isReferenceTo(element)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element
    }

    private fun ownerMembers(presenters: TemplatePresenters, role: Role): List<LookupElementBuilder> {
        val ownerClass = ownerClass(presenters) ?: return emptyList()
        return presenters.members(ownerClass, role).map { (name, _) -> LookupElementBuilder.create(name).withIcon(AllIcons.Actions.Execute) }
    }

    /** The class of the component the owner's factory returns. */
    private fun ownerClass(presenters: TemplatePresenters): PhpClass? {
        val factory = owner?.let { presenters.resolveComponent(it) } ?: return null
        val type = factory.type.types.firstOrNull() ?: return null
        return PhpIndex.getInstance(factory.project).getAnyByFQN(type).firstOrNull()
    }

    private fun templatePresenters(): TemplatePresenters? {
        return (element.containingFile as? LatteFile)?.let { TemplatePresenters.of(it) }
    }
}
