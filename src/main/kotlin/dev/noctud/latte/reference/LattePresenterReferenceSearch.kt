package dev.noctud.latte.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import dev.noctud.latte.LatteFileType
import dev.noctud.latte.presenter.NetteConventions
import dev.noctud.latte.presenter.NetteConventions.Role
import dev.noctud.latte.psi.LatteControl
import dev.noctud.latte.psi.LatteLink
import dev.noctud.latte.psi.elements.LatteControlPartElement
import dev.noctud.latte.psi.elements.LatteLinkPartElement
import dev.noctud.latte.reference.references.LatteControlReference
import dev.noctud.latte.reference.references.LatteLinkReference

/**
 * Find usages of presenters, actions, signals and components in templates. The references refuse
 * [PsiReference.isReferenceTo] so that a rename in PHP leaves them alone, which means the platform
 * cannot find them by itself: each template that mentions the name is asked here instead.
 */
class LattePresenterReferenceSearch : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val target = parameters.elementToSearch
        if (target !is PhpClass && target !is Method) {
            return
        }
        if (!NetteConventions.isUsedIn(target.project)) {
            return
        }

        val scope = latteOnly(parameters.effectiveSearchScope)
        if (target is PhpClass) {
            processPresenter(target, scope, consumer)
        } else if (target is Method) {
            processLinkedMethod(target, scope, consumer)
            processComponentMethod(target, scope, consumer)
        }
    }

    private fun processPresenter(presenter: PhpClass, scope: SearchScope, consumer: Processor<in PsiReference>) {
        if (!NetteConventions.isPresenter(presenter)) {
            return
        }

        val name = NetteConventions.linkName(presenter)
        if (name.isNotEmpty()) {
            processLinks(name, presenter, scope, consumer) { it.link.contains(name) }
        }
    }

    /** An action or a signal, linked to by its name or, from its own template, by `this`. */
    private fun processLinkedMethod(method: Method, scope: SearchScope, consumer: Processor<in PsiReference>) {
        val name = NetteConventions.memberName(method, Role.ACTION) ?: NetteConventions.memberName(method, Role.SIGNAL) ?: return
        if (method.containingClass == null) {
            return
        }

        for (word in listOf(name, "this")) {
            processLinks(word, method, scope, consumer) { it.link == "this" || it.link.contains(name) }
        }
    }

    /** A component factory or a render mode. */
    private fun processComponentMethod(method: Method, scope: SearchScope, consumer: Processor<in PsiReference>) {
        val name = NetteConventions.memberName(method, Role.COMPONENT) ?: NetteConventions.memberName(method, Role.RENDER_MODE) ?: return

        PsiSearchHelper.getInstance(method.project).processElementsWithWord({ element, _ ->
            val control = element.parent as? LatteControl
            if (control != null && control.control.contains(name)) {
                for (reference in partReferences<LatteControlPartElement>(control)) {
                    if (reference is LatteControlReference && reference.isRefTo(method)) {
                        consumer.process(reference)
                    }
                }
            }
            true
        }, scope, name, UsageSearchContext.ANY, true)
    }

    private fun processLinks(word: String, target: PsiElement, scope: SearchScope, consumer: Processor<in PsiReference>, accepts: (LatteLink) -> Boolean) {
        PsiSearchHelper.getInstance(target.project).processElementsWithWord({ element, _ ->
            val link = element.parent as? LatteLink
            if (link != null && accepts(link)) {
                for (reference in partReferences<LatteLinkPartElement>(link)) {
                    if (reference is LatteLinkReference && reference.isRefTo(target)) {
                        consumer.process(reference)
                    }
                }
            }
            true
        }, scope, word, UsageSearchContext.ANY, true)
    }

    private inline fun <reified T : PsiElement> partReferences(parent: PsiElement): List<PsiReference> {
        return parent.children.filterIsInstance<T>().flatMap { it.references.asList() }
    }

    private fun latteOnly(scope: SearchScope): SearchScope {
        return if (scope is GlobalSearchScope) GlobalSearchScope.getScopeRestrictedByFileTypes(scope, LatteFileType.INSTANCE) else scope
    }
}
