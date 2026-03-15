package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LatteMacroModifier;

public interface LatteFilterStub extends StubElement<LatteMacroModifier> {
    String getModifierName();
}
