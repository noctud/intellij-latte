package dev.noctud.latte.psi.elements;

import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpStatement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface LattePhpExpressionElement extends LattePsiElement {

    @NotNull
    NettePhpType getReturnType();

    @NotNull
    List<LattePhpStatement> getPhpStatementList();

    int getPhpArrayLevel();

}
