package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LattePhpTypeStub;
import dev.noctud.latte.indexes.stubs.types.LattePhpTypeStubType;
import dev.noctud.latte.psi.LattePhpType;
import dev.noctud.latte.psi.LatteTypes;

public class LattePhpTypeStubImpl extends StubBase<LattePhpType> implements LattePhpTypeStub {
    private final String phpType;

    public LattePhpTypeStubImpl(final StubElement parent, final String phpType) {
        super(parent, (LattePhpTypeStubType) LatteTypes.PHP_TYPE);
        this.phpType = phpType;
    }

    @Override
    public String getPhpType() {
        return phpType;
    }
}
