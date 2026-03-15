package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LattePhpStaticVariableStub;
import dev.noctud.latte.indexes.stubs.types.LattePhpStaticVariableStubType;
import dev.noctud.latte.psi.LattePhpStaticVariable;
import dev.noctud.latte.psi.LatteTypes;

public class LattePhpStaticVariableStubImpl extends StubBase<LattePhpStaticVariable> implements LattePhpStaticVariableStub {
    private final String variableName;

    public LattePhpStaticVariableStubImpl(final StubElement parent, final String variableName) {
        super(parent, (LattePhpStaticVariableStubType) LatteTypes.PHP_STATIC_VARIABLE);
        this.variableName = variableName;
    }

    @Override
    public String getVariableName() {
        return variableName;
    }
}
