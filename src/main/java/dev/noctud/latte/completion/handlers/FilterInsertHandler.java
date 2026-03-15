package dev.noctud.latte.completion.handlers;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import dev.noctud.latte.LatteLanguage;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.psi.LatteMacroModifier;
import dev.noctud.latte.settings.LatteFilterSettings;
import org.jetbrains.annotations.NotNull;

public class FilterInsertHandler implements InsertHandler<LookupElement> {

    private static final FilterInsertHandler instance = new FilterInsertHandler();

    public FilterInsertHandler() {
        super();
    }

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
        PsiElement element = context.getFile().findElementAt(context.getStartOffset());
        if (element != null && element.getLanguage() == LatteLanguage.INSTANCE) {
            PsiElement parent = element.getParent();

            LatteFilterSettings filter = null;
            if (parent instanceof LatteMacroModifier) {
                String name = ((LatteMacroModifier) parent).getModifierName();
                filter = LatteConfiguration.getInstance(element.getProject()).getFilter(name);
            }

            Editor editor = context.getEditor();
            if (filter != null && filter.getModifierInsert() != null && filter.getModifierInsert().length() > 0) {
                CaretModel caretModel = editor.getCaretModel();
                String text = editor.getDocument().getText();

                int offset = caretModel.getOffset();

                int lastBraceOffset = text.indexOf(":", offset);
                int endOfLineOffset = text.indexOf("\n", offset);

                if (endOfLineOffset == -1) {
                    endOfLineOffset = text.length();
                }
                if (lastBraceOffset == -1 || lastBraceOffset > endOfLineOffset) {
                    EditorModificationUtil.insertStringAtCaret(editor, filter.getModifierInsert());
                    caretModel.moveToOffset(offset + 1);
                }
            }
            PsiDocumentManager.getInstance(context.getProject()).commitDocument(editor.getDocument());
        }
    }

    public static FilterInsertHandler getInstance() {
        return instance;
    }
}
