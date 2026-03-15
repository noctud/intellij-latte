package dev.noctud.latte.editor;

import com.intellij.ide.structureView.*;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiFile;
import dev.noctud.latte.psi.LatteFile;
import org.jetbrains.annotations.NotNull;

public class LatteStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {
    public LatteStructureViewModel(PsiFile psiFile) {
        super(psiFile, new LatteStructureViewTreeElement(psiFile));
    }

    @NotNull
    public Sorter[] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return element instanceof LatteFile;
    }
}
