package dev.noctud.latte.indexes.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import dev.noctud.latte.psi.LattePhpStaticVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class LattePhpStaticVariableIndex extends StringStubIndexExtension<LattePhpStaticVariable> {
    public static final StubIndexKey<String, LattePhpStaticVariable> KEY = StubIndexKey.createIndexKey("latte.phpStaticVariable.index");

    private static final LattePhpStaticVariableIndex ourInstance = new LattePhpStaticVariableIndex();

    public static LattePhpStaticVariableIndex getInstance() {
        return ourInstance;
    }

    @Override
    public int getVersion() {
        return super.getVersion() + 3;
    }

    @Override
    @NotNull
    public StubIndexKey<String, LattePhpStaticVariable> getKey() {
        return KEY;
    }

    public Collection<LattePhpStaticVariable> getElement(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(getKey(), key, project, scope, LattePhpStaticVariable.class);
    }
}
