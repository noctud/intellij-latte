package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.icons.LatteIcons;
import dev.noctud.latte.indexes.stubs.LatteFilterStub;
import dev.noctud.latte.psi.LatteMacroContent;
import dev.noctud.latte.psi.LattePhpInBrackets;
import dev.noctud.latte.psi.elements.LatteMacroModifierElement;
import dev.noctud.latte.psi.impl.LatteStubElementImpl;
import dev.noctud.latte.psi.impl.LattePsiImplUtil;
import dev.noctud.latte.settings.LatteFilterSettings;
import dev.noctud.latte.utils.LatteUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteTypes;

import javax.swing.*;

public abstract class LatteMacroModifierElementImpl extends LatteStubElementImpl<LatteFilterStub> implements LatteMacroModifierElement {

    private @Nullable String modifierName = null;
    private @Nullable PsiElement identifier = null;

    public LatteMacroModifierElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public LatteMacroModifierElementImpl(final LatteFilterStub stub, final IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        modifierName = null;
        identifier = null;
    }

    @Nullable
    public LatteMacroContent getMacroContent() {
        return findChildByClass(LatteMacroContent.class);
    }

    @Override
    public String getModifierName() {
        if (modifierName == null) {
            final LatteFilterStub stub = getStub();
            if (stub != null) {
                modifierName = stub.getModifierName();
                return modifierName;
            }

            PsiElement found = getTextElement();
            modifierName = found != null ? LatteUtil.normalizeMacroModifier(found.getText()) : null;
        }
        return modifierName;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        if (identifier == null) {
            identifier = LattePsiImplUtil.findFirstChildWithType(this, LatteTypes.T_MACRO_FILTERS);
        }
        return identifier;
    }

    @Override
    public boolean isVariableModifier() {
        LattePhpInBrackets variableModifier = PsiTreeUtil.getParentOfType(this, LattePhpInBrackets.class);
        return variableModifier != null;
    }

    @Override
    public @Nullable PsiElement getTextElement() {
        return getNameIdentifier();
    }

    @Nullable
    public LatteFilterSettings getMacroModifier() {
        return LatteConfiguration.getInstance(getProject()).getFilter(getModifierName());
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return LatteIcons.MODIFIER;
    }
}
