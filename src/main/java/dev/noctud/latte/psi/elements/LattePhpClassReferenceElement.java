package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpClassStub;
import dev.noctud.latte.psi.LattePhpClassUsage;
import org.jetbrains.annotations.NotNull;

public interface LattePhpClassReferenceElement extends BaseLattePhpElement, StubBasedPsiElement<LattePhpClassStub> {

    String getClassName();

    @NotNull
    LattePhpClassUsage getPhpClassUsage();

    /**
     * Whether the name stands where PHP reads a global constant, like {@code {=\VERSION}}. The
     * lexer gives every name starting with a backslash to this element, so only the position tells
     * a constant from a class: alone in its statement, with no {@code ::} after it, not a type and
     * not after {@code new} or {@code instanceof}.
     */
    boolean isConstantPosition();

}
