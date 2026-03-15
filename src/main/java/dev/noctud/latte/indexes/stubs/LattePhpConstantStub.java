package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpConstant;

public interface LattePhpConstantStub extends StubElement<LattePhpConstant> {
    String getConstantName();
}
