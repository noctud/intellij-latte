package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import dev.noctud.latte.indexes.stubs.LattePhpStaticVariableStub;
import dev.noctud.latte.php.LattePhpVariableUtil;
import dev.noctud.latte.psi.LatteElementFactory;
import dev.noctud.latte.psi.elements.LattePhpStaticVariableElement;
import dev.noctud.latte.psi.impl.LatteStubPhpElementImpl;
import dev.noctud.latte.psi.impl.LattePsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteTypes;

public abstract class LattePhpStaticVariableElementImpl extends LatteStubPhpElementImpl<LattePhpStaticVariableStub> implements LattePhpStaticVariableElement {

    private @Nullable String name = null;
    private @Nullable String variableName = null;
    private @Nullable PsiElement identifier = null;

    public LattePhpStaticVariableElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public LattePhpStaticVariableElementImpl(final LattePhpStaticVariableStub stub, final IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        this.name = null;
        this.variableName = null;
        this.identifier = null;
    }

    @Override
    public String getPhpElementName() {
        return getVariableName();
    }

    @Override
    public String getVariableName() {
        if (variableName == null) {
            final LattePhpStaticVariableStub stub = getStub();
            if (stub != null) {
                variableName = stub.getVariableName();
                return variableName;
            }

            PsiElement found = getTextElement();
            variableName = found != null ? LattePhpVariableUtil.normalizePhpVariable(found.getText()) : null;
        }
        return variableName;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        if (identifier == null) {
            identifier = LattePsiImplUtil.findFirstChildWithType(this, LatteTypes.T_MACRO_ARGS_VAR);
        }
        return identifier;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        ASTNode keyNode = getFirstChild().getNode();
        PsiElement property = LatteElementFactory.createStaticVariable(getProject(), name);
        if (property == null) {
            return this;
        }
        return LatteElementFactory.replaceChildNode(this, property, keyNode);
    }

    @Override
    public String getName() {
        if (name == null) {
            PsiElement found = getNameIdentifier();
            name = found != null ? found.getText() : null;
        }
        return name;
    }
}
