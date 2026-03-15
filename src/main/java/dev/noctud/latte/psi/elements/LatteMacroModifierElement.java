package dev.noctud.latte.psi.elements;

import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.util.IncorrectOperationException;
import dev.noctud.latte.indexes.stubs.LatteFilterStub;
import dev.noctud.latte.settings.LatteFilterSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LatteMacroModifierElement extends LattePsiNamedElement, StubBasedPsiElement<LatteFilterStub> {

    @Override
    default PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        return this;
    }

    String getModifierName();

    @Nullable
    LatteFilterSettings getMacroModifier();

    @Nullable PsiElement getTextElement();

    boolean isVariableModifier();

}
