package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpMethod;

public interface LattePhpMethodStub extends StubElement<LattePhpMethod> {
    String getMethodName();
}
