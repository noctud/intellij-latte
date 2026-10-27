package dev.noctud.latte.psi.impl.elements

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import dev.noctud.latte.psi.LatteControl
import dev.noctud.latte.psi.elements.LatteControlPartElement
import dev.noctud.latte.psi.impl.LattePsiElementImpl
import dev.noctud.latte.reference.references.LatteControlReference
import dev.noctud.latte.reference.references.LatteControlReference.Kind

abstract class LatteControlPartElementImpl(node: ASTNode) : LattePsiElementImpl(node), LatteControlPartElement {

    private var cachedName: String? = null

    override fun subtreeChanged() {
        super.subtreeChanged()
        cachedName = null
    }

    override fun getNameIdentifier(): PsiElement {
        return this
    }

    override fun getName(): String {
        return cachedName ?: text.also { cachedName = it }
    }

    /**
     * The references depend on the whole control, that is on our siblings, so they cannot be cached
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

    /**
     * `menu-items:sidebar` is the component `menu`, its subcomponent `items`, and the render mode
     * `sidebar` of what is before the colon. Only a part that is one whole name gets a reference.
     */
    private fun computeReferences(): Array<PsiReference> {
        if (name == ":" || name == "-") {
            return PsiReference.EMPTY_ARRAY
        }

        val path = (parent as? LatteControl)?.text ?: return PsiReference.EMPTY_ARRAY
        val start = startOffsetInParent
        val segment = NAME.findAll(path).firstOrNull { it.range.first == start && it.range.last + 1 == start + textLength }
            ?: return PsiReference.EMPTY_ARRAY

        val component = path.substringBefore(':')
        val text = segment.value.replace(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED, "")
        val range = TextRange(0, textLength)

        if (start > component.length) {
            return arrayOf(LatteControlReference(this, range, true, text, Kind.RENDER_MODE, component.split('-')))
        }

        // The names before this one, `[menu, items]` for `badge` in `menu-items-badge`.
        val owners = component.substring(0, start).split('-').dropLast(1)
        val kind = if (owners.isEmpty()) Kind.COMPONENT else Kind.SUBCOMPONENT

        return arrayOf(LatteControlReference(this, range, true, text, kind, owners))
    }

    private companion object {
        val NAME = Regex("[^:-]+")
    }
}
