package dev.noctud.latte.indexes.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import dev.noctud.latte.psi.LatteMacroModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class LatteFilterIndex extends StringStubIndexExtension<LatteMacroModifier> {
    public static final StubIndexKey<String, LatteMacroModifier> KEY = StubIndexKey.createIndexKey("latte.filter.index");

    private static final LatteFilterIndex ourInstance = new LatteFilterIndex();

    public static LatteFilterIndex getInstance() {
        return ourInstance;
    }

    @Override
    public int getVersion() {
        return super.getVersion() + 3;
    }

    @Override
    @NotNull
    public StubIndexKey<String, LatteMacroModifier> getKey() {
        return KEY;
    }

    public Collection<LatteMacroModifier> getElement(@NotNull String key, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return StubIndex.getElements(getKey(), key, project, scope, LatteMacroModifier.class);
    }
}
