package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import dev.noctud.latte.psi.LatteMacroTag;
import dev.noctud.latte.psi.elements.LattePairMacroElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class LatteMacroTagImpl extends LattePsiElementImpl implements LattePairMacroElement {

    public LatteMacroTagImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public LatteMacroTag getMacroOpenTag() {
        return getMacroTagList().stream().findFirst().orElse(null);
    }
}
