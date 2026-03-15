package dev.noctud.latte.psi.elements;

import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpStatementFirstPart;
import dev.noctud.latte.psi.LattePhpStatementPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LattePhpStatementElement extends LattePsiElement {

    @NotNull LattePhpStatementFirstPart getPhpStatementFirstPart();

    @NotNull List<LattePhpStatementPart> getPhpStatementPartList();

    NettePhpType getReturnType();

    boolean isPhpVariableOnly();

    boolean isPhpClassReferenceOnly();

    @Nullable BaseLattePhpElement getLastPhpElement();

}
