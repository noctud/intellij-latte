package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import dev.noctud.latte.psi.LatteHtmlOpenTag;
import dev.noctud.latte.psi.elements.LatteHtmlTagContainerElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import dev.noctud.latte.psi.impl.LattePsiImplUtil;
import org.jetbrains.annotations.NotNull;

public abstract class LatteHtmlTagContainerImpl extends LattePsiElementImpl implements LatteHtmlTagContainerElement {

    public LatteHtmlTagContainerImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public LatteHtmlOpenTag getHtmlOpenTag() {
        return LattePsiImplUtil.getHtmlOpenTag(this);
    }
}
