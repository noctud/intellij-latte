package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpMethodStub;
import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;

public interface LattePhpMethodElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpMethodStub> {

    default NettePhpType getPrevReturnType() {
        return LattePhpTypeDetector.detectPrevPhpType(this);
    }

    String getMethodName();

    boolean isStatic();

    boolean isFunction();

}
