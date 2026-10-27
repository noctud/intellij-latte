package dev.noctud.latte.psi.impl.elements

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import dev.noctud.latte.ext.isCapitalized
import dev.noctud.latte.psi.elements.LatteLinkElement
import dev.noctud.latte.psi.elements.LatteLinkPartElement
import dev.noctud.latte.psi.impl.LattePsiElementImpl
import dev.noctud.latte.reference.references.LatteLinkReference

abstract class LatteLinkPartElementImpl(node: ASTNode) : LattePsiElementImpl(node), LatteLinkPartElement {

    private var cachedName: String? = null

    override fun subtreeChanged() {
        super.subtreeChanged()
        cachedName = null
    }

    override fun getParentLink(): LatteLinkElement {
        return parent as LatteLinkElement
    }

    override fun getNameIdentifier(): PsiElement {
        return this
    }

    override fun getName(): String {
        return cachedName ?: text.also { cachedName = it }
    }

    /**
     * The references depend on the whole link, that is on our siblings, so they cannot be cached
     * in a field cleared by our own [subtreeChanged] - an edit in a sibling never reaches it. The
     * platform also asks for them from several threads at once, which a lazily populated field
     * cannot survive.
     *
     * A copy goes out rather than the cached array itself: a caller that sorts or rewrites what it
     * is handed would otherwise change the answer for everyone after it.
     */
    override fun getReferences(): Array<PsiReference> {
        return CachedValuesManager.getCachedValue(this) { CachedValueProvider.Result.create(computeReferences(), this) }.clone()
    }

    private fun computeReferences(): Array<PsiReference> {
        if (name == ":" || name == "/") {
            return PsiReference.EMPTY_ARRAY
        }

        // Every capitalised name in the link is a presenter, wherever this part is.
        val presenters = parentLink.link
            .replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "")
            .replace("/", "")
            .trim()
            .split(":")
            .filter { it.isNotEmpty() && it.isCapitalized() }

        val currentPresenter = presenters.lastOrNull()
        val previousPresenters = presenters.dropLast(1)

        val text = name.replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "")
        return arrayOf(LatteLinkReference(this, TextRange(0, textLength), true, text, currentPresenter, previousPresenters))
    }
}
