package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteTypes;
import dev.noctud.latte.psi.elements.LatteFilePathElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import dev.noctud.latte.psi.impl.LattePsiImplUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class LatteFilePathElementImpl extends LattePsiElementImpl implements LatteFilePathElement {
    private @Nullable PsiElement identifier = null;

    public LatteFilePathElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        identifier = null;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        if (identifier == null) {
            identifier = LattePsiImplUtil.findFirstChildWithType(this, LatteTypes.T_FILE_PATH);
        }

        return identifier;
    }

    @Override
    public @NotNull String getFilePath() {
        return this.getText();
    }

    /**
     * The platform asks for the references from several threads at once, which a lazily populated
     * field cannot survive - the second thread is handed a list the first one is still filling.
     *
     * <p>A copy goes out rather than the cached array itself: a caller that sorts or rewrites what
     * it is handed would otherwise change the answer for everyone after it.
     */
    @Override
    public PsiReference @NotNull [] getReferences() {
        return CachedValuesManager.getCachedValue(
            this,
            () -> CachedValueProvider.Result.create(computeReferences(), this)
        ).clone();
    }

    private PsiReference @NotNull [] computeReferences() {
        List<PsiReference> references = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int textRangeIndex = 0;

        for (String entity : this.getFilePath().trim().split("/")) {
            if (!entity.isEmpty()) {
                int finalTextRangeIndex = textRangeIndex;

                references.add(new PsiReferenceBase<PsiElement>(this, new TextRange(finalTextRangeIndex, finalTextRangeIndex + entity.length()), true) {
                    private final String directoryPath = current.append('/').toString();
                    private final String path = current.append(entity).toString();

                    @Override
                    public @Nullable PsiElement resolve() {
                        PsiDirectory containingDir = myElement.getContainingFile().getContainingDirectory();
                        if (containingDir == null) {
                            return null;
                        }
                        VirtualFile virtual = VirtualFileManager.getInstance().findFileByUrl("file://" + containingDir.getVirtualFile().getPath() + path);
                        if (virtual == null) {
                            return null;
                        }

                        PsiFileSystemItem fileOrDirectory = PsiManager.getInstance(myElement.getProject()).findDirectory(virtual);
                        if (fileOrDirectory == null) {
                            fileOrDirectory = PsiManager.getInstance(myElement.getProject()).findFile(virtual);
                        }

                        return fileOrDirectory;
                    }

                    @Override
                    public Object @NotNull [] getVariants() {
                        PsiDirectory variantsDir = getContainingFile().getOriginalFile().getContainingDirectory();
                        if (variantsDir == null) {
                            return new Object[0];
                        }
                        String target = "file://" + variantsDir.getVirtualFile().getPath() + directoryPath;
                        if (target.endsWith("/")) {
                            target = target.substring(0, target.length() - 1);
                        }

                        VirtualFile virtual = VirtualFileManager.getInstance().findFileByUrl(target);
                        if (virtual == null || !virtual.isDirectory()) {
                            return new Object[0];
                        }

                        PsiDirectory directory = PsiManager.getInstance(getProject()).findDirectory(virtual);
                        if (directory == null) {
                            return new Object[0];
                        }

                        List<PsiFileSystemItem> items = new ArrayList<>();
                        for (VirtualFile file : virtual.getChildren()) {
                            if (!file.isDirectory() && !file.getName().endsWith(".latte")) {
                                continue;
                            }

                            PsiFileSystemItem fileOrDirectory = PsiManager.getInstance(getProject()).findDirectory(file);
                            if (fileOrDirectory == null) {
                                fileOrDirectory = PsiManager.getInstance(getProject()).findFile(file);
                            }

                            items.add(fileOrDirectory);
                        }

                        return items.toArray();
                    }
                });
                textRangeIndex += entity.length() + 1;

            } else {
                textRangeIndex++;
            }
        }

        return references.toArray(new PsiReference[0]);
    }
}
