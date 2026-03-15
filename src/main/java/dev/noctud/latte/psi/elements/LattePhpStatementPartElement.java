package dev.noctud.latte.psi.elements;

import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LattePhpStatementPartElement extends LattePsiElement {

    default @NotNull NettePhpType getReturnType() {
        return LattePhpTypeDetector.detectPhpType(this);
    }

    @NotNull LattePhpStatement getPhpStatement();

    @Nullable LattePhpStatementPartElement getPrevPhpStatementPart();

    @Nullable BaseLattePhpElement getPhpElement();

}
