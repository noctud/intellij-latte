package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpConstantStub;
import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;

public interface LattePhpConstantElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpConstantStub> {

    default NettePhpType getPrevReturnType() {
        return LattePhpTypeDetector.detectPrevPhpType(this);
    }

    String getConstantName();

}
