package dev.noctud.latte.reference.references;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import dev.noctud.latte.indexes.LatteIndexUtil;
import dev.noctud.latte.psi.LattePhpConstant;
import dev.noctud.latte.psi.elements.BaseLattePhpElement;
import dev.noctud.latte.php.LattePhpUtil;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpEnumCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LattePhpConstantReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
    private final String key;
    private final Project project;
    private final Collection<PhpClass> phpClasses;

    public LattePhpConstantReference(@NotNull LattePhpConstant element, TextRange textRange) {
        super(element, textRange);
        key = element.getConstantName();
        project = element.getProject();
        phpClasses = element.getPrevReturnType().getPhpClasses(project);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean b) {
        if (phpClasses.size() == 0) {
            return new ResolveResult[0];
        }

        final Collection<LattePhpConstant> methods = LatteIndexUtil.findConstantsByName(project, key);
        List<ResolveResult> results = new ArrayList<>();
        for (BaseLattePhpElement method : methods) {
            if (method.getPrevReturnType().hasClass(phpClasses)) {
                results.add(new PsiElementResolveResult(method));
            }
        }

        List<Field> fields = LattePhpUtil.getFieldsForPhpElement((BaseLattePhpElement) getElement(), project);
        String name = ((BaseLattePhpElement) getElement()).getPhpElementName();
        for (Field field : fields) {
            if (field.getName().equals(name)) {
                results.add(new PsiElementResolveResult(field));
            }
        }

        List<PhpEnumCase> enumCases = LattePhpUtil.getEnumCasesForPhpElement((BaseLattePhpElement) getElement(), project);
        for (PhpEnumCase enumCase : enumCases) {
            if (enumCase.getName().equals(name)) {
                results.add(new PsiElementResolveResult(enumCase));
            }
        }

        return results.toArray(new ResolveResult[0]);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        List<Field> fields = LattePhpUtil.getFieldsForPhpElement((BaseLattePhpElement) getElement(), project);
        if (fields.size() > 0) {
            return fields.get(0);
        }

        List<PhpEnumCase> enumCases = LattePhpUtil.getEnumCasesForPhpElement((BaseLattePhpElement) getElement(), project);
        return enumCases.size() > 0 ? enumCases.get(0) : null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (element instanceof LattePhpConstant) {
            Collection<PhpClass> originalClasses = ((LattePhpConstant) element).getPrevReturnType().getPhpClasses(project);
            if (originalClasses.size() > 0) {
                for (PhpClass originalClass : originalClasses) {
                    if (LattePhpUtil.isReferenceTo(originalClass, multiResolve(false), project, ((LattePhpConstant) element).getConstantName())) {
                        return true;
                    }
                }
            }
        }

        if (element instanceof Field) {
            PhpClass originalClass = ((Field) element).getContainingClass();
            if (originalClass == null) {
                return false;
            }
            return LattePhpUtil.isReferenceTo(originalClass, multiResolve(false), project, ((Field) element).getName());
        }

        if (element instanceof PhpEnumCase) {
            PhpClass originalClass = ((PhpEnumCase) element).getContainingClass();
            if (originalClass == null) {
                return false;
            }
            return LattePhpUtil.isReferenceTo(originalClass, multiResolve(false), project, ((PhpEnumCase) element).getName());
        }

        return false;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) {
        if (getElement() instanceof LattePhpConstant) {
            ((LattePhpConstant) getElement()).setName(newName);
        }
        return getElement();
    }

}
