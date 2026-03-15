package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import dev.noctud.latte.psi.elements.LattePhpTypedPartElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import org.jetbrains.annotations.NotNull;

public abstract class LattePhpTypedPartElementImpl extends LattePsiElementImpl implements LattePhpTypedPartElement {

    public LattePhpTypedPartElementImpl(@NotNull ASTNode node) {
        super(node);
    }
}
