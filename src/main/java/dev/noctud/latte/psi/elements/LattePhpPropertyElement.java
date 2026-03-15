package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpPropertyStub;
import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;

public interface LattePhpPropertyElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpPropertyStub> {

    default NettePhpType getPrevReturnType() {
        return LattePhpTypeDetector.detectPrevPhpType(this);
    }

    String getPropertyName();

}
