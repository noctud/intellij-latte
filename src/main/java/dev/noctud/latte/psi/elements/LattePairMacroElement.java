package dev.noctud.latte.psi.elements;

import dev.noctud.latte.psi.LatteMacroTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface LattePairMacroElement extends LattePsiElement {

    @NotNull
    List<LatteMacroTag> getMacroTagList();

    @Nullable
    LatteMacroTag getMacroOpenTag();

    @NotNull
    LatteMacroTag getOpenTag();

    @Nullable
    LatteMacroTag getCloseTag();
}
