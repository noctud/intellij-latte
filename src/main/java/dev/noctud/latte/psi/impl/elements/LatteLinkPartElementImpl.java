package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
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
    private @Nullable List<PsiReference> references = null;
    private @Nullable String name = null;

    public LatteLinkPartElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        references = null;
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

    @Override
    public PsiReference @NotNull [] getReferences() {
        if (references == null) {
            references = new ArrayList<>();

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
            references.add(new LatteLinkReference(this, new TextRange(0, getTextLength()), true, clean, currentPresenter, previousPresenters));
        }

        return references.toArray(new PsiReference[0]);
    }
}
