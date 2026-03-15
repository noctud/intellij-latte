package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpStaticVariable;

public interface LattePhpStaticVariableStub extends StubElement<LattePhpStaticVariable> {
    String getVariableName();
}
