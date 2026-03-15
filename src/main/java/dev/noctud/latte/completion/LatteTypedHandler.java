package dev.noctud.latte.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import dev.noctud.latte.LatteLanguage;

/**
 * Triggers completion autopopup when typing '$' inside Latte macro content so that
 * variable completion appears at "{$}" without requiring manual invocation.
 */
public class LatteTypedHandler extends TypedHandlerDelegate {
    @Override
    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        // Only react for Latte files (or mixed where Latte is among languages)
        if (!file.getViewProvider().getLanguages().contains(LatteLanguage.INSTANCE)) {
            return Result.CONTINUE;
        }

        if (c == '$') {
            int offset = Math.max(0, editor.getCaretModel().getOffset() - 1);

            PsiElement elementAtCaret = file.findElementAt(offset);
            if (elementAtCaret != null) {
                PsiElement prevElement = file.findElementAt(offset - 1);
                if (prevElement != null) {
                    if (prevElement.getText().equals("{") || prevElement.getText().equals("{}")) {
                        AutoPopupController.getInstance(project).scheduleAutoPopup(editor);
                        return Result.STOP;
                    }
                }
            }
        }

        return Result.CONTINUE;
    }
}
