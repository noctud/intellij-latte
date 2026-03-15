package dev.noctud.latte.intentions;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class CreateMissingFile implements LocalQuickFix {
    private final @NotNull VirtualFile currentFile;
    private final String absolutePath;

    public CreateMissingFile(@NotNull VirtualFile currentFile, String absolutePath) {
        this.currentFile = currentFile;
        this.absolutePath = absolutePath;
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return "Create file " + new File(absolutePath).getName();
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        if (VirtualFileManager.getInstance().findFileByUrl(absolutePath) != null) {
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            File file = new File(absolutePath.replace("file://", ""));

            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new RuntimeException("Cannot create directory " + parent.getAbsolutePath());
            }

            try {
                VirtualFile parentDirectory = VirtualFileManager.getInstance().refreshAndFindFileByUrl(absolutePath.substring(0, absolutePath.lastIndexOf('/')));
                if (parentDirectory == null) {
                    throw new RuntimeException("Cannot find parent directory " + absolutePath.substring(0, absolutePath.lastIndexOf('/')));
                }

                VirtualFile newFile = parentDirectory.createChildData(this, new File(absolutePath.substring(absolutePath.lastIndexOf('/') + 1)).getName());

                String firstLine = LoadTextUtil.loadText(currentFile).toString().split("\n")[0];
                if (firstLine.startsWith("{templateType")) {
                    newFile.setBinaryContent((firstLine + "\n").getBytes());
                }

                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, newFile, LoadTextUtil.loadText(currentFile).toString().length()), true);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
