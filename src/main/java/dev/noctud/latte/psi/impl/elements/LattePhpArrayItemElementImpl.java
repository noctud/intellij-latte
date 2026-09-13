package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.psi.LattePhpArrayValue;
import dev.noctud.latte.psi.elements.LattePhpArrayItemElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LattePhpArrayItemElementImpl extends LattePsiElementImpl implements LattePhpArrayItemElement {

    public LattePhpArrayItemElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable LattePhpArrayValue getKey() {
        List<LattePhpArrayValue> values = PsiTreeUtil.getChildrenOfTypeAsList(this, LattePhpArrayValue.class);
        return values.size() == 2 ? values.get(0) : null;
    }

    @Override
    public @Nullable LattePhpArrayValue getValue() {
        List<LattePhpArrayValue> values = PsiTreeUtil.getChildrenOfTypeAsList(this, LattePhpArrayValue.class);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }
}
