package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.elements.LatteLinkElement;
import dev.noctud.latte.psi.elements.LatteLinkPartElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import dev.noctud.latte.reference.references.LatteLinkReference;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class LatteLinkPartElementImpl extends LattePsiElementImpl implements LatteLinkPartElement {
    private @Nullable String name = null;

    public LatteLinkPartElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        name = null;
    }

    @Override
    public @NotNull LatteLinkElement getParentLink() {
        return (LatteLinkElement) getParent();
    }

    @Override
    public PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public @NotNull String getName() {
        if (name == null) {
            name = getText();
        }
        return name;
    }

    /**
     * The references depend on the whole link, that is on our siblings, so they cannot be cached
     * in a field cleared by our own {@link #subtreeChanged()} - an edit in a sibling never reaches
     * it. The platform also asks for them from several threads at once, which a lazily populated
     * field cannot survive.
     *
     * <p>A copy goes out rather than the cached array itself: a caller that sorts or rewrites what
     * it is handed would otherwise change the answer for everyone after it.
     */
    @Override
    public PsiReference @NotNull [] getReferences() {
        return CachedValuesManager.getCachedValue(
            this,
            () -> CachedValueProvider.Result.create(computeReferences(), this)
        ).clone();
    }

    private PsiReference @NotNull [] computeReferences() {
        if (getName().equals(":") || getName().equals("/")) {
            return PsiReference.EMPTY_ARRAY;
        }

        String wholeText = getParentLink().getLink();

        // Build presenters list from whole text (capitalized tokens)
        List<String> presenters = new ArrayList<>();
        for (String presenter : wholeText.replace("IntellijIdeaRulezzz", "").replace("/", "").trim().split(":")) {
            if (!presenter.isEmpty() && presenter.equals(StringUtils.capitalize(presenter))) {
                presenters.add(presenter);
            }
        }

        String currentPresenter = !presenters.isEmpty() ? presenters.get(presenters.size() - 1) : null;
        List<String> previousPresenters = new ArrayList<>(presenters);

        if (!previousPresenters.isEmpty()) {
            previousPresenters.remove(previousPresenters.size() - 1);
        }

        if (currentPresenter != null && currentPresenter.equals("IntellijIdeaRulezzz")) {
            currentPresenter = null;
        }

        String clean = getName().replace("IntellijIdeaRulezzz", "");
        return new PsiReference[]{
            new LatteLinkReference(this, new TextRange(0, getTextLength()), true, clean, currentPresenter, previousPresenters)
        };
    }
}
