package dev.noctud.latte.indexes.stubs;

import com.intellij.psi.stubs.StubElement;
import dev.noctud.latte.psi.LattePhpClassReference;

public interface LattePhpClassStub extends StubElement<LattePhpClassReference> {
    String getClassName();
}
