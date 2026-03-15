package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LattePhpPropertyStub;
import dev.noctud.latte.indexes.stubs.types.LattePhpPropertyStubType;
import dev.noctud.latte.psi.LattePhpProperty;
import dev.noctud.latte.psi.LatteTypes;

public class LattePhpPropertyStubImpl extends StubBase<LattePhpProperty> implements LattePhpPropertyStub {
    private final String propertyName;

    public LattePhpPropertyStubImpl(final StubElement parent, final String propertyName) {
        super(parent, (LattePhpPropertyStubType) LatteTypes.PHP_PROPERTY);
        this.propertyName = propertyName;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }
}
