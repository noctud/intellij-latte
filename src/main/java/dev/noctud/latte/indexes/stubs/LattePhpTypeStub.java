package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpType;

public interface LattePhpTypeStub extends StubElement<LattePhpType> {
    String getPhpType();
}
