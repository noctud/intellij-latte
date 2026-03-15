package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.psi.LatteMacroTag;
import dev.noctud.latte.psi.elements.LattePairMacroElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class LattePairMacroImpl extends LattePsiElementImpl implements LattePairMacroElement {

    public LattePairMacroImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull List<LatteMacroTag> getMacroTagList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, LatteMacroTag.class);
    }

    @Nullable
    public LatteMacroTag getMacroOpenTag() {
        return getMacroTagList().stream().findFirst().orElse(null);
    }

    @Override
    public @Nullable LatteMacroTag getCloseTag() {
        List<LatteMacroTag> tags = getMacroTagList();
        return tags.size() < 2 ? null : tags.get(1);
    }

    public @NotNull LatteMacroTag getOpenTag() {
        return getMacroTagList().get(0);
    }
}
