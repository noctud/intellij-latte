package dev.noctud.latte.indexes.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import dev.noctud.latte.psi.LattePhpNamespaceReference;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class LattePhpNamespaceIndex extends StringStubIndexExtension<LattePhpNamespaceReference> {
    public static final StubIndexKey<String, LattePhpNamespaceReference> KEY = StubIndexKey.createIndexKey("latte.phpNamespace.index");

    private static final LattePhpNamespaceIndex ourInstance = new LattePhpNamespaceIndex();

    public static LattePhpNamespaceIndex getInstance() {
        return ourInstance;
    }

    @Override
    public int getVersion() {
        return super.getVersion() + 3;
    }

    @Override
    @NotNull
    public StubIndexKey<String, LattePhpNamespaceReference> getKey() {
        return KEY;
    }

    public Collection<LattePhpNamespaceReference> getElement(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(getKey(), key, project, scope, LattePhpNamespaceReference.class);
    }
}
