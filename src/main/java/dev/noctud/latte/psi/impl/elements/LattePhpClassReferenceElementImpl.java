package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import dev.noctud.latte.indexes.stubs.LattePhpClassStub;
import dev.noctud.latte.psi.LattePhpStatement;
import dev.noctud.latte.psi.LattePhpStatementFirstPart;
import dev.noctud.latte.psi.LatteTypes;
import dev.noctud.latte.psi.elements.LattePhpClassReferenceElement;
import dev.noctud.latte.psi.impl.LatteStubPhpElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LattePhpClassReferenceElementImpl extends LatteStubPhpElementImpl<LattePhpClassStub> implements LattePhpClassReferenceElement {

    private @Nullable String name = null;
    private @Nullable String className = null;

    public LattePhpClassReferenceElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public LattePhpClassReferenceElementImpl(final LattePhpClassStub stub, final IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        name = null;
        className = null;
        getPhpClassUsage().reset();
    }

    @Override
    public String getPhpElementName() {
        return getClassName();
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return getPhpClassUsage().getNameIdentifier();
    }

    @Override
    public String getClassName() {
        if (className == null) {
            final LattePhpClassStub stub = getStub();
            if (stub != null) {
                className = stub.getClassName();
                return className;
            }
            className = getPhpClassUsage().getClassName();
        }
        return className;
    }

    /**
     * The {@code new} or {@code instanceof} sits outside the statement, the same as for
     * {@link LattePhpMethodElementImpl#isConstructorCall()}, so it is looked for before it.
     */
    @Override
    public boolean isConstantPosition() {
        PsiElement firstPart = getParent();
        if (!(firstPart instanceof LattePhpStatementFirstPart) || !(firstPart.getParent() instanceof LattePhpStatement)) {
            return false;
        }
        LattePhpStatement statement = (LattePhpStatement) firstPart.getParent();
        if (!statement.getPhpStatementPartList().isEmpty()) {
            return false;
        }
        // ::class makes no statement part, the keyword is not a constant name, so the :: is left after the statement
        PsiElement next = PsiTreeUtil.skipWhitespacesAndCommentsForward(statement);
        if (next != null && next.getNode().getElementType() == LatteTypes.T_PHP_DOUBLE_COLON) {
            return false;
        }
        PsiElement prev = PsiTreeUtil.skipWhitespacesAndCommentsBackward(statement);
        if (prev == null) {
            return true;
        }
        IElementType type = prev.getNode().getElementType();
        return type != LatteTypes.T_PHP_NEW && !(type == LatteTypes.T_PHP_KEYWORD && prev.getText().equals("instanceof"));
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        return this;
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
