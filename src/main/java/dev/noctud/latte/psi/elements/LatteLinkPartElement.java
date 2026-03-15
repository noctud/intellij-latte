package dev.noctud.latte.psi.elements;

import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public interface LatteLinkPartElement extends LattePsiNamedElement {
    @NotNull
    LatteLinkElement getParentLink();

    @Override
    default PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        return this;
    }
}
