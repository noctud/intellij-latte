package dev.noctud.latte.editorActions;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import dev.noctud.latte.LatteLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles individual keystrokes.
 */
public class LatteTypedHandler extends TypedHandlerDelegate {

    private static final Map<Character, Character> pairs = new HashMap<Character, Character>(3);
    private static final Set<Character> chars = new HashSet<Character>(3);

    static {
        pairs.put('{', '}');
        pairs.put('(', ')');
        pairs.put('[', ']');
        chars.add('}');
        chars.add(')');
        chars.add(']');
    }

    @Override
    public @NotNull Result beforeCharTyped(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {
        // ignores typing '}' before '}'
        if (chars.contains(charTyped) && file.getViewProvider().getLanguages().contains(LatteLanguage.INSTANCE)) {
            CaretModel caretModel = editor.getCaretModel();
            CharSequence charsSeq = editor.getDocument().getCharsSequence();
            int offset = caretModel.getOffset();
            if (offset < charsSeq.length() && charsSeq.charAt(offset) == charTyped) {
                caretModel.moveToOffset(offset + 1);
                return Result.STOP;
            }
        }
        return super.beforeCharTyped(charTyped, project, editor, file, fileType);
    }

    @Override
    public @NotNull Result charTyped(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        // auto-inserts '}' after typing '{'
        if (pairs.containsKey(charTyped) && file.getViewProvider().getLanguages().contains(LatteLanguage.INSTANCE)) {
            int offset = editor.getCaretModel().getOffset();
            CharSequence charsSeq = editor.getDocument().getCharsSequence();
            Character pairChar = pairs.get(charTyped);
            if (offset >= charsSeq.length() || charsSeq.charAt(offset) != pairChar) {
                editor.getDocument().insertString(offset, pairChar.toString());
                return Result.STOP;
            }
        }

        return super.charTyped(charTyped, project, editor, file);
    }
}
