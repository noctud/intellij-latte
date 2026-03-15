package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LattePhpClassStub;
import dev.noctud.latte.indexes.stubs.types.LattePhpClassStubType;
import dev.noctud.latte.psi.LattePhpClassReference;
import dev.noctud.latte.psi.LatteTypes;

public class LattePhpClassStubImpl extends StubBase<LattePhpClassReference> implements LattePhpClassStub {
    private final String className;

    public LattePhpClassStubImpl(final StubElement parent, final String className) {
        super(parent, (LattePhpClassStubType) LatteTypes.PHP_CLASS_REFERENCE);
        this.className = className;
    }

    @Override
    public String getClassName() {
        return className;
    }
}
