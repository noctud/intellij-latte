package dev.noctud.latte.reference;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import dev.noctud.latte.LatteFileType;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LattePhpStaticVariable;
import dev.noctud.latte.psi.LattePhpVariable;
import dev.noctud.latte.php.LattePhpUtil;
import dev.noctud.latte.utils.LatteUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class LatteReferenceSearch extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

    @Override
    public void processQuery(ReferencesSearch.SearchParameters searchParameters, @NotNull Processor<? super PsiReference> processor) {
        PsiElement target = searchParameters.getElementToSearch();
        SearchScope scope = latteOnly(searchParameters.getEffectiveSearchScope());

        if (target instanceof Field) {
            processField((Field) target, scope, processor);
        }
    }

    private static SearchScope latteOnly(SearchScope scope) {
        if (scope instanceof GlobalSearchScope) {
            return GlobalSearchScope.getScopeRestrictedByFileTypes((GlobalSearchScope) scope, LatteFileType.INSTANCE);
        }

        return scope;
    }

    private void processField(@NotNull Field field, @NotNull SearchScope searchScope, @NotNull Processor<? super PsiReference> processor) {
        ApplicationManager.getApplication().runReadAction(() -> {
            if (field.isConstant()) {
                return;
            }
            String fieldName = field.getName();

            PsiSearchHelper.getInstance(field.getProject()).processElementsWithWordAsync((psiElement, i) -> {
                PsiElement currentMethod = psiElement.getParent();

                if (currentMethod instanceof LattePhpStaticVariable) {
                    String variableName = ((LattePhpStaticVariable) currentMethod).getVariableName();
                    if (!variableName.equals(fieldName)) { // performance
                        return true;
                    }

                    for (PsiReference ref : currentMethod.getReferences()) {
                        if (ref.isReferenceTo(field)) {
                            processor.process(ref);
                        }
                    }

                } else if (currentMethod instanceof LattePhpVariable && field.getContainingClass() != null) {
                    String variableName = ((LattePhpVariable) currentMethod).getVariableName();
                    if (!variableName.equals(fieldName)) { // performance
                        return true;
                    }

                    NettePhpType type = LatteUtil.findFirstLatteTemplateType(currentMethod.getContainingFile());
                    if (type == null) {
                        return true;
                    }

                    Collection<PhpClass> classes = type.getPhpClasses(psiElement.getProject());
                    for (PhpClass phpClass : classes) {
                        if (LattePhpUtil.isReferenceFor(field.getContainingClass(), phpClass)) {
                            for (PsiReference ref : currentMethod.getReferences()) {
                                if (ref.isReferenceTo(field)) {
                                    processor.process(ref);
                                }
                            }
                        }
                    }
                }

                return true;
            }, searchScope, "$" + fieldName, UsageSearchContext.IN_CODE, true);
        });
    }
}
