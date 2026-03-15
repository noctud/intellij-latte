package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteTypes;
import dev.noctud.latte.psi.elements.LatteFilePathElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;
import dev.noctud.latte.psi.impl.LattePsiImplUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class LatteFilePathElementImpl extends LattePsiElementImpl implements LatteFilePathElement {
    private @Nullable List<PsiReference> references = null;
    private @Nullable PsiElement identifier = null;

    public LatteFilePathElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        identifier = null;
        references = null;
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

    @Override
    public PsiReference @NotNull [] getReferences() {
        if (references == null) {
            references = new ArrayList<>();
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
                            VirtualFile virtual = VirtualFileManager.getInstance().findFileByUrl("file://" + myElement.getContainingFile().getContainingDirectory().getVirtualFile().getPath() + path);
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
                            String target = "file://" + getContainingFile().getOriginalFile().getContainingDirectory().getVirtualFile().getPath() + directoryPath;
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
        }

        return references.toArray(new PsiReference[0]);
    }
}
