package dev.noctud.latte.indexes.extensions;

import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;
import dev.noctud.latte.psi.LattePhpType;
import org.jetbrains.annotations.NotNull;

public class LattePhpTypeIndex extends StringStubIndexExtension<LattePhpType> {
    public static final StubIndexKey<String, LattePhpType> KEY = StubIndexKey.createIndexKey("latte.phpType.index");

    private static final LattePhpTypeIndex ourInstance = new LattePhpTypeIndex();

    public static LattePhpTypeIndex getInstance() {
        return ourInstance;
    }

    @Override
    public int getVersion() {
        return super.getVersion() + 3;
    }

    @Override
    @NotNull
    public StubIndexKey<String, LattePhpType> getKey() {
        return KEY;
    }
}
