package dev.noctud.latte.indexes.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.indexes.stubs.LatteFilterStub;
import dev.noctud.latte.indexes.stubs.types.LatteFilterStubType;
import dev.noctud.latte.psi.LatteMacroModifier;
import dev.noctud.latte.psi.LatteTypes;

public class LatteFilterStubImpl extends StubBase<LatteMacroModifier> implements LatteFilterStub {
    private final String filterName;

    public LatteFilterStubImpl(final StubElement parent, final String filterName) {
        super(parent, (LatteFilterStubType) LatteTypes.MACRO_MODIFIER);
        this.filterName = filterName;
    }

    @Override
    public String getModifierName() {
        return filterName;
    }
}
