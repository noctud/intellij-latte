package dev.noctud.latte.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import dev.noctud.latte.completion.handlers.PhpVariableInsertHandler;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.*;
import dev.noctud.latte.psi.elements.BaseLattePhpElement;
import dev.noctud.latte.utils.LatteTagsUtil;
import dev.noctud.latte.utils.LatteTypesUtil;
import dev.noctud.latte.utils.LatteUtil;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.insert.PhpFieldInsertHandler;
import com.jetbrains.php.completion.insert.PhpMethodInsertHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class LattePhpCompletionProvider extends BaseLatteCompletionProvider {
    private final LattePhpFunctionCompletionProvider functionCompletionProvider;
    private final LattePhpClassCompletionProvider classCompletionProvider;
    private final LattePhpNamespaceCompletionProvider namespaceCompletionProvider;
    private final LatteVariableCompletionProvider variableCompletionProvider;

    public LattePhpCompletionProvider() {
        super();
        functionCompletionProvider = new LattePhpFunctionCompletionProvider();
        classCompletionProvider = new LattePhpClassCompletionProvider();
        namespaceCompletionProvider = new LattePhpNamespaceCompletionProvider();
        variableCompletionProvider = new LatteVariableCompletionProvider();
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        PsiElement current = parameters.getPosition();
        PsiElement element = current.getParent();
        if (
            element instanceof LattePhpStaticVariable
                || element instanceof LattePhpConstant
                || (element instanceof LattePhpMethod && ((LattePhpMethod) element).isStatic())) {
            attachPhpCompletions(parameters, context, result, (BaseLattePhpElement) element, true);

        } else if (element instanceof LattePhpProperty || (element instanceof LattePhpMethod && !((LattePhpMethod) element).isStatic())) {
            attachPhpCompletions(parameters, context, result, (BaseLattePhpElement) element, false);

        } else if (
            !(element instanceof LatteMacroModifier)
                && !(element instanceof LattePhpString)
                && !(element instanceof LatteFilePath)
                && !(element instanceof LatteLinkPart)
                && !(element instanceof LatteControlPart)
                && !(parameters.getPosition().getParent() instanceof LatteMacroCloseTag)
        ) {
            String prefix = result.getPrefixMatcher().getPrefix();

            boolean looksLikeVariable = prefix.startsWith("$") || prefix.contains("$");
            boolean isLattePhpVariable = (element instanceof LattePhpVariable) && !((LattePhpVariable) element).isDefinition();
            int invocation = parameters.getInvocationCount();
            boolean allowHeavy = invocation >= 2 || prefix.length() >= 2;

            // Class name contexts: allow heavy providers only when allowed
            if (isInClassDefinition(element)) {
                if (allowHeavy) {
                    classCompletionProvider.addCompletions(parameters, context, result);
                    //namespaceCompletionProvider.addCompletions(parameters, context, result);
                } else {
                    result.restartCompletionOnAnyPrefixChange();
                }
                return;
            }

            boolean parentType = LatteUtil.matchParentMacroName(element, LatteTagsUtil.Type.VAR_TYPE.getTagName());
            boolean parentTemplateType = LatteUtil.matchParentMacroName(element, LatteTagsUtil.Type.TEMPLATE_TYPE.getTagName());
            if (parentType || parentTemplateType || LatteUtil.matchParentMacroName(element, LatteTagsUtil.Type.VAR.getTagName())) {
                attachVarTypes(result);
                if (parentType || parentTemplateType || isInTypeDefinition(current)) {
                    if (allowHeavy) {
                        classCompletionProvider.addCompletions(parameters, context, result);
                        //namespaceCompletionProvider.addCompletions(parameters, context, result);
                    } else {
                        result.restartCompletionOnAnyPrefixChange();
                    }
                    return;
                }
            }

            // Variable-first behavior: keep autopopup cheap
            if (isLattePhpVariable || looksLikeVariable) {
                variableCompletionProvider.addCompletions(parameters, context, result);
                if (invocation >= 2) {
                    functionCompletionProvider.addCompletions(parameters, context, result);
                    classCompletionProvider.addCompletions(parameters, context, result);
                    //namespaceCompletionProvider.addCompletions(parameters, context, result);
                }
                return;
            }

            // General PHP content: variables first, heavy providers only when allowed
            variableCompletionProvider.addCompletions(parameters, context, result);
            if (allowHeavy) {
                classCompletionProvider.addCompletions(parameters, context, result);
                //namespaceCompletionProvider.addCompletions(parameters, context, result);
                functionCompletionProvider.addCompletions(parameters, context, result);
            } else {
                result.restartCompletionOnAnyPrefixChange();
            }
        }
    }

    private void attachVarTypes(@NotNull CompletionResultSet result) {
        for (String nativeTypeHint : LatteTypesUtil.getNativeTypeHints()) {
            result.addElement(LookupElementBuilder.create(nativeTypeHint));
        }
    }

    private void attachPhpCompletions(
        @NotNull CompletionParameters parameters,
        ProcessingContext context,
        @NotNull CompletionResultSet result,
        @NotNull BaseLattePhpElement psiElement,
        boolean isStatic
    ) {
        NettePhpType type = psiElement.getPrevReturnType();

        Collection<PhpClass> phpClasses = type.getPhpClasses(psiElement.getProject());
        if (phpClasses.size() == 0) {
            if (psiElement instanceof LattePhpMethod && (psiElement.getPhpStatementPart() == null || psiElement.getPhpStatementPart().getPrevPhpStatementPart() == null)) {
                functionCompletionProvider.addCompletions(parameters, context, result);
            }
            return;
        }

        boolean isMagicPrefixed = result.getPrefixMatcher().getPrefix().startsWith("__");
        for (PhpClass phpClass : phpClasses) {
            if (isStatic) {
                for (PhpEnumCase enumCase : phpClass.getEnumCases()) {
                    PhpLookupElement lookupItem = getPhpLookupElement(enumCase, enumCase.getName());
                    lookupItem.handler = PhpFieldInsertHandler.getInstance();
                    result.addElement(lookupItem);
                }
            }

            for (Method method : phpClass.getMethods()) {
                PhpModifier modifier = method.getModifier();
                if (modifier.isPublic() && canShowCompletionElement(isStatic, modifier)) {
                    String name = method.getName();
                    if (!isMagicPrefixed && parameters.getInvocationCount() <= 1 && LatteTypesUtil.isExcludedCompletion(name)) {
                        continue;
                    }
                    PhpLookupElement lookupItem = getPhpLookupElement(method, name);
                    lookupItem.handler = PhpMethodInsertHandler.getInstance();
                    result.addElement(lookupItem);
                }
            }

            for (Field field : phpClass.getFields()) {
                PhpModifier modifier = field.getModifier();
                if (modifier.isPublic()) {
                    if (isStatic) {
                        if (field.isConstant()) {
                            PhpLookupElement lookupItem = getPhpLookupElement(field, field.getName());
                            lookupItem.handler = PhpFieldInsertHandler.getInstance();
                            result.addElement(lookupItem);

                        } else if (modifier.isStatic()) {
                            PhpLookupElement lookupItem = getPhpLookupElement(field, "$" + field.getName());
                            lookupItem.handler = PhpVariableInsertHandler.getInstance();
                            result.addElement(lookupItem);
                        }

                    } else {
                        if (!field.isConstant() && !modifier.isStatic()) {
                            PhpLookupElement lookupItem = getPhpLookupElement(field, field.getName());
                            lookupItem.handler = PhpFieldInsertHandler.getInstance();
                            result.addElement(lookupItem);
                        }
                    }
                }
            }

            if (isStatic) {
                for (String nativeConstant : LatteTypesUtil.getNativeClassConstants()) {
                    result.addElement(LookupElementBuilder.create(nativeConstant));
                }
            }
        }
    }

    private boolean isInClassDefinition(@Nullable PsiElement element) {
        return element != null && element.getNode().getElementType() == LatteTypes.PHP_CLASS_USAGE;
    }

    private boolean isInTypeDefinition(@Nullable PsiElement element) {
        return element != null
            && (element.getPrevSibling() == null || (LatteTypesUtil.phpTypeTokens.contains(element.getPrevSibling().getNode().getElementType())))
            && element.getNode().getElementType() != LatteTypes.T_MACRO_ARGS_VAR;
    }

    private boolean canShowCompletionElement(boolean isStatic, @NotNull PhpModifier modifier) {
        return (isStatic && modifier.isStatic()) || (!isStatic && !modifier.isStatic());
    }

}
