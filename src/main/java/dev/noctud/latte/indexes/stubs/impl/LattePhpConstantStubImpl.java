package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LattePhpConstantStub;
import dev.noctud.latte.indexes.stubs.types.LattePhpConstantStubType;
import dev.noctud.latte.psi.LattePhpConstant;
import dev.noctud.latte.psi.LatteTypes;

public class LattePhpConstantStubImpl extends StubBase<LattePhpConstant> implements LattePhpConstantStub {
    private final String constantName;

    public LattePhpConstantStubImpl(final StubElement parent, final String constantName) {
        super(parent, (LattePhpConstantStubType) LatteTypes.PHP_CONSTANT);
        this.constantName = constantName;
    }

    @Override
    public String getConstantName() {
        return constantName;
    }
}
