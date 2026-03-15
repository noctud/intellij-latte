package dev.noctud.latte.psi.elements;

import com.intellij.psi.StubBasedPsiElement;
import dev.noctud.latte.indexes.stubs.LattePhpTypeStub;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpTypePart;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface LattePhpTypeElement extends LattePsiElement, StubBasedPsiElement<LattePhpTypeStub> {

    @NotNull
    List<LattePhpTypePart> getPhpTypePartList();

    @NotNull
    NettePhpType getReturnType();

}
