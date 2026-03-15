package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpClassStub;
import dev.noctud.latte.psi.LattePhpClassUsage;
import org.jetbrains.annotations.NotNull;

public interface LattePhpClassReferenceElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpClassStub> {

    String getClassName();

    @NotNull
    LattePhpClassUsage getPhpClassUsage();

}
