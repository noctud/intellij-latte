package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpStaticVariableStub;
import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;

public interface LattePhpStaticVariableElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpStaticVariableStub> {

    default NettePhpType getPrevReturnType() {
        return LattePhpTypeDetector.detectPrevPhpType(this);
    }

    String getVariableName();

}
