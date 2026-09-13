package dev.noctud.latte.psi.elements;

import dev.noctud.latte.psi.LattePhpArrayValue;
import org.jetbrains.annotations.Nullable;

/**
 * One item of an array literal. The parser reads the key and the value as two expressions of the
 * same kind, so the item says which is which.
 */
public interface LattePhpArrayItemElement extends LattePsiElement {

    /** The key of {@code 'x' => 1}, or null for an item written without one. */
    @Nullable
    LattePhpArrayValue getKey();

    /** The value: the only expression of an item without a key, the second of one with a key. */
    @Nullable
    LattePhpArrayValue getValue();
}
