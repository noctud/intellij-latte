package dev.noctud.latte.indexes.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import dev.noctud.latte.psi.LattePhpClassReference;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class LattePhpClassIndex extends StringStubIndexExtension<LattePhpClassReference> {
    public static final StubIndexKey<String, LattePhpClassReference> KEY = StubIndexKey.createIndexKey("latte.phpClass.index");

    private static final LattePhpClassIndex ourInstance = new LattePhpClassIndex();

    public static LattePhpClassIndex getInstance() {
        return ourInstance;
    }

    @Override
    public int getVersion() {
        return super.getVersion() + 3;
    }

    @Override
    @NotNull
    public StubIndexKey<String, LattePhpClassReference> getKey() {
        return KEY;
    }

    public Collection<LattePhpClassReference> getElement(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(getKey(), key, project, scope, LattePhpClassReference.class);
    }
}
