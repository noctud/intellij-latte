package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpNamespaceReference;

public interface LattePhpNamespaceStub extends StubElement<LattePhpNamespaceReference> {
    String getNamespaceName();
}
