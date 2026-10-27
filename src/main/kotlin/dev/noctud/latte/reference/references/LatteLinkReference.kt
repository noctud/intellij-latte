package dev.noctud.latte.reference.references

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.latte.ext.isCapitalized
import dev.noctud.latte.presenter.NetteConventions
import dev.noctud.latte.presenter.NetteConventions.Role
import dev.noctud.latte.presenter.TemplatePresenters
import dev.noctud.latte.psi.LatteFile
import dev.noctud.latte.psi.elements.LatteLinkElement

/**
 * One part of a link destination: a presenter (`Product`), an action (`edit`) or a signal
 * (`delete!`). Capitalised names are presenters, and the last of them is the presenter the action
 * or signal belongs to - `currentPresenter`, with the ones before it in `previousPresenters`.
 */
class LatteLinkReference(
    element: PsiElement,
    rangeInElement: TextRange,
    soft: Boolean,
    private val text: String,
    private val currentPresenter: String?,
    private val previousPresenters: List<String>,
) : PsiReferenceBase<PsiElement>(element, rangeInElement, soft) {

    override fun resolve(): PsiElement? {
        val presenters = templatePresenters() ?: return null
        if (text == ":" || text.contains("/")) {
            return null
        }

        val presenter = linkedPresenter(presenters)
        return when {
            text.endsWith("!") -> presenters.resolveSignal(presenter, text.removeSuffix("!"))
            !text.isCapitalized() -> presenters.resolveAction(presenter, text)
            else -> presenters.resolvePresenter(text, previousPresenters, text != currentPresenter)
        }
    }

    override fun getVariants(): Array<Any> {
        val presenters = templatePresenters() ?: return emptyArray()
        var presenter = linkedPresenter(presenters)

        val parent = element.parent
        val linkText = (parent as? LatteLinkElement)?.link ?: parent?.text ?: ""
        val cleanLink = linkText.replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "")

        val variants = mutableListOf<LookupElement>()
        if (text.isEmpty() || text.isCapitalized()) {
            variants += presenters.presentersForCompletion(previousPresenters.lastOrNull(), linkText.startsWith(":"), !cleanLink.trim().contains(":"))
        }

        if ((presenter == null || !presenter.isAbstract) && (text.isEmpty() || !text.isCapitalized()) && cleanLink != ":") {
            val templatePresenter = presenters.templatePresenter(previousPresenters)
            if (presenter == null) {
                presenter = templatePresenter
            }

            if (presenter != null) {
                val isCurrent = templatePresenter != null && presenter.isEquivalentTo(templatePresenter)
                if (isCurrent) {
                    variants += LookupElementBuilder.create("this").withTailText(" in " + presenter.name).withIcon(AllIcons.Actions.Execute)
                }

                variants += presenters.actionsForCompletion(presenter)
                if (isCurrent) {
                    variants += presenters.signalsForCompletion(presenter)
                }
            }
        }

        return variants.toTypedArray()
    }

    /** False, so renaming a PHP class or method does not try to rename the link. */
    override fun isReferenceTo(element: PsiElement): Boolean {
        return false
    }

    /**
     * What [isReferenceTo] would say if it did not refuse, for find usages. The names are compared
     * first so that most links are turned away without resolving them.
     */
    fun isRefTo(element: PsiElement): Boolean {
        if (element is PhpClass && currentPresenter != null) {
            val name = NetteConventions.linkName(element)
            if (name != currentPresenter && name !in previousPresenters) {
                return false
            }
        } else if (element is Method) {
            val name = NetteConventions.memberName(element, Role.ACTION) ?: NetteConventions.memberName(element, Role.SIGNAL) ?: element.name
            if (!text.startsWith(name) && !text.contains(":$name") && text != "this") {
                return false
            }
        }

        return super.isReferenceTo(element)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element
    }

    /** The presenter the action or signal of this link belongs to, if the link names one. */
    private fun linkedPresenter(presenters: TemplatePresenters): PhpClass? {
        if (currentPresenter != null) {
            return presenters.resolvePresenter(currentPresenter, previousPresenters, false)
        }

        val last = previousPresenters.lastOrNull() ?: return null
        return presenters.resolvePresenter(last, previousPresenters.dropLast(1), false)
    }

    private fun templatePresenters(): TemplatePresenters? {
        return (element.containingFile as? LatteFile)?.let { TemplatePresenters.of(it) }
    }
}
