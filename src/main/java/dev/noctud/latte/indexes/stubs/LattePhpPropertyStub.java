package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpProperty;

public interface LattePhpPropertyStub extends StubElement<LattePhpProperty> {
    String getPropertyName();
}
