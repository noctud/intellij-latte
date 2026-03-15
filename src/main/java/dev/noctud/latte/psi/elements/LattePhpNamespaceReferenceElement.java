package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpNamespaceStub;

public interface LattePhpNamespaceReferenceElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpNamespaceStub> {

    public abstract String getNamespaceName();

}
