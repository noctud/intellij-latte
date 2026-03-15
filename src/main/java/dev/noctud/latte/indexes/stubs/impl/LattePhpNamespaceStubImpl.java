package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LattePhpNamespaceStub;
import dev.noctud.latte.indexes.stubs.types.LattePhpNamespaceStubType;
import dev.noctud.latte.psi.LattePhpNamespaceReference;
import dev.noctud.latte.psi.LatteTypes;

public class LattePhpNamespaceStubImpl extends StubBase<LattePhpNamespaceReference> implements LattePhpNamespaceStub {
    private final String namespaceName;

    public LattePhpNamespaceStubImpl(final StubElement parent, final String constantName) {
        super(parent, (LattePhpNamespaceStubType) LatteTypes.PHP_NAMESPACE_REFERENCE);
        this.namespaceName = constantName;
    }

    @Override
    public String getNamespaceName() {
        return namespaceName;
    }
}
