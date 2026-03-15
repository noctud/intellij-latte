package dev.noctud.latte.psi.elements;

import com.intellij.psi.PsiElement;
import dev.noctud.latte.php.LattePhpTypeDetector;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpArrayUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface BaseLattePhpElement extends LattePsiNamedElement {

    default @NotNull List<LattePhpArrayUsage> getPhpArrayUsageList() {
        return Collections.emptyList();
    }

    default NettePhpType getPrevReturnType() {
        return getReturnType();
    }

    default NettePhpType getReturnType() {
        return LattePhpTypeDetector.detectPhpType(this);
    }

    String getPhpElementName();

    int getPhpArrayLevel();

    @Nullable PsiElement getTextElement();

    @Nullable LattePhpStatementPartElement getPhpStatementPart();

}
