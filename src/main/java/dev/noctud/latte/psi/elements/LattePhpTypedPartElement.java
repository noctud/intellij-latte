package dev.noctud.latte.psi.elements;

import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpType;
import dev.noctud.latte.psi.LattePhpVariable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LattePhpTypedPartElement extends LattePsiElement {

    @Nullable
    LattePhpType getPhpType();

    @NotNull
    LattePhpVariable getPhpVariable();

    default @NotNull NettePhpType getReturnType() {
        return LattePhpTypeDetector.detectPhpType(this);
    }

}
