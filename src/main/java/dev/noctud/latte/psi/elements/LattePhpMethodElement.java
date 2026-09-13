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

    /**
     * Whether the call is the {@code new Foo(...)} of a constructor rather than a call to a
     * function of that name. The lexer gives an unqualified name followed by a bracket to this
     * element whichever it is, and only the {@code new} in front of it tells them apart.
     */
    boolean isConstructorCall();

}
