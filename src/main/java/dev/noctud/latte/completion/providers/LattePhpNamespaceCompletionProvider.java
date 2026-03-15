package dev.noctud.latte.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import dev.noctud.latte.completion.handlers.PhpNamespaceInsertHandler;
import dev.noctud.latte.psi.LattePhpContent;
import dev.noctud.latte.php.LattePhpUtil;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class LattePhpNamespaceCompletionProvider extends BaseLatteCompletionProvider {

    public LattePhpNamespaceCompletionProvider() {
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters params, ProcessingContext context, @NotNull CompletionResultSet result) {
        PsiElement curr = params.getPosition().getOriginalElement();
        if (PsiTreeUtil.getParentOfType(curr, LattePhpContent.class) == null) {
            return;
        }

        /*

        Maybe autocomplete namespaces only if \ is before?
        Currently it's bugged, if you type App\Mod.. and accept the Model namespace,
        you'll end with App\App\Model, that's why it is disabled for now


        String prefix = result.getPrefixMatcher().getPrefix();
        boolean hasBackslash = false;
        int offset = params.getOffset();
        int candidateIndex = offset - prefix.length() - 1;
        CharSequence chars = params.getEditor().getDocument().getCharsSequence();
        if (candidateIndex >= 0 && candidateIndex < chars.length()) {
            hasBackslash = chars.charAt(candidateIndex) == '\\';
        }

        if (!hasBackslash) {
            return;
        }*/

        String namespaceName = getNamespaceName(curr);
        Collection<String> namespaceNames = LattePhpUtil.getAllExistingNamespacesByName(curr.getProject(), namespaceName);
        Collection<PhpNamespace> namespaces = LattePhpUtil.getAlNamespaces(curr.getProject(), namespaceNames);
        for (PhpNamespace namespace : namespaces) {
            PhpLookupElement lookupItem = getPhpLookupElement(namespace, null, getTypeText(namespace.getParentNamespaceName()));
            lookupItem.handler = PhpNamespaceInsertHandler.getInstance();
            result.addElement(lookupItem);
        }
    }

    @Nullable
    private String getTypeText(String parentNamespace) {
        if (parentNamespace.length() > 1) {
            if (parentNamespace.startsWith("\\")) {
                parentNamespace = parentNamespace.substring(1);
            }
            if (parentNamespace.endsWith("\\") && parentNamespace.length() > 1) {
                parentNamespace = parentNamespace.substring(0, parentNamespace.length() - 1);
            }
            return " [" + parentNamespace + "]";
        }
        return null;
    }

}
