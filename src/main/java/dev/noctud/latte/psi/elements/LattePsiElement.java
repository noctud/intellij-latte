package dev.noctud.latte.psi.elements;

import com.intellij.psi.PsiElement;
import dev.noctud.latte.psi.LatteFile;
import org.jetbrains.annotations.Nullable;

public interface LattePsiElement extends PsiElement {

    @Nullable LatteFile getLatteFile();

}
