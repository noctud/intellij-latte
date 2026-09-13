package dev.noctud.latte.inspections;

import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.intentions.AddCustomLatteFunction;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.LattePhpMethod;
import dev.noctud.latte.settings.LatteFunctionSettings;
import dev.noctud.latte.php.LattePhpUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodUsagesInspection extends BaseLocalInspectionTool {

    @NotNull
    @Override
    public String getShortName() {
        return "LatteMethodUsages";
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        if (!(file instanceof LatteFile)) {
            return null;
        }

        final List<ProblemDescriptor> problems = new ArrayList<>();
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof LattePhpMethod) {
                    if (((LattePhpMethod) element).isFunction()) {
                        processFunction((LattePhpMethod) element, problems, manager, isOnTheFly);

                    } else {
                        processMethod((LattePhpMethod) element, problems, manager, isOnTheFly);
                    }

                } else {
                    super.visitElement(element);
                }
            }
        });

        return problems.toArray(new ProblemDescriptor[0]);
    }

    private void processFunction(
        LattePhpMethod element,
        @NotNull List<ProblemDescriptor> problems,
        @NotNull final InspectionManager manager,
        final boolean isOnTheFly
    ) {
        String name = element.getMethodName();
        if (name == null) {
            return;
        }

        if (element.isConstructorCall()) {
            // new Foo(...) names a class, and the lexer hands an unqualified name followed by a
            // bracket to the same element a function call uses. Looking it up among the functions
            // reported every constructor written that way as a function that does not exist.
            // A qualified name is parsed as a class reference instead and never came here, so
            // ClassUsagesInspection reports that half.
            String className = LattePhpUtil.normalizeClassName(name);
            Collection<PhpClass> classes = LattePhpUtil.getClassesByFQN(element.getProject(), className);
            if (classes.size() == 0) {
                addProblem(manager, problems, getElementToLook(element), "Undefined class '" + className + "'", isOnTheFly);

            } else {
                // The same checks ClassUsagesInspection makes for the qualified spelling.
                for (PhpClass phpClass : classes) {
                    if (phpClass.isDeprecated()) {
                        addDeprecated(manager, problems, getElementToLook(element), "Used class '" + className + "' is marked as deprecated", isOnTheFly);
                        break;

                    } else if (phpClass.isInternal()) {
                        addDeprecated(manager, problems, getElementToLook(element), "Used class '" + className + "' is marked as internal", isOnTheFly);
                        break;
                    }
                }
            }
            return;
        }

        LatteFunctionSettings customFunction = LatteConfiguration.getInstance(element.getProject()).getFunction(name);
        if (customFunction != null) {
            return;
        }

        Collection<Function> existing = LattePhpUtil.getFunctionByName(element.getProject(), name);
        if (existing.size() == 0) {
            LocalQuickFix addFunctionFix = IntentionManager.getInstance().convertToFix(new AddCustomLatteFunction(name));
            addProblem(manager, problems, getElementToLook(element), "Function '" + name + "' not found", isOnTheFly, addFunctionFix);

        } else {
            for (Function function : existing) {
                if (function.isDeprecated()) {
                    addDeprecated(manager, problems, getElementToLook(element), "Function '" + name + "' is deprecated", isOnTheFly);
                }
                if (function.isInternal()) {
                    addDeprecated(manager, problems, getElementToLook(element), "Function '" + name + "' is internal", isOnTheFly);
                }
            }
        }
    }

    private void processMethod(
        LattePhpMethod element,
        @NotNull List<ProblemDescriptor> problems,
        @NotNull final InspectionManager manager,
        final boolean isOnTheFly
    ) {
        NettePhpType phpType = element.getPrevReturnType();

        Collection<PhpClass> phpClasses = phpType.getPhpClasses(element.getProject());
        if (phpClasses.size() == 0) {
            // No class was found for the type at all, so nothing was read and nothing can be
            // missing from it. Saying the method is absent would turn "the class is unknown" -
            // an unindexed project, a package that is not installed - into a broken template.
            // The other three inspections that look a name up on a type do the same.
            return;
        }

        boolean isFound = false;
        String methodName = element.getMethodName();
        for (PhpClass phpClass : phpClasses) {
            for (Method method : phpClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    if (method.getModifier().isPrivate()) {
                        addProblem(manager, problems, getElementToLook(element), "Used private method '" + methodName + "'", isOnTheFly);
                    } else if (method.getModifier().isProtected()) {
                        addProblem(manager, problems, getElementToLook(element), "Used protected method '" + methodName + "'", isOnTheFly);
                    } else if (method.isDeprecated()) {
                        addDeprecated(manager, problems, getElementToLook(element), "Used method '" + methodName + "' is marked as deprecated", isOnTheFly);
                    } else if (method.isInternal()) {
                        addDeprecated(manager, problems, getElementToLook(element), "Used method '" + methodName + "' is marked as internal", isOnTheFly);
                    }

                    String description;
                    boolean isStatic = element.isStatic();
                    if (isStatic && !method.getModifier().isStatic()) {
                        description = "Method '" + methodName + "' is not static but called statically";
                        addProblem(manager, problems, getElementToLook(element), description, isOnTheFly);

                    } else if (!isStatic && method.getModifier().isStatic()) {
                        description = "Method '" + methodName + "' is static but called non statically";
                        addProblem(manager, problems, getElementToLook(element), description, isOnTheFly);
                    }
                    isFound = true;
                }
            }
        }

        if (!isFound) {
            addProblem(manager, problems, getElementToLook(element), "Method '" + methodName + "' not found for type '" + phpType.toString() + "'", isOnTheFly);
        }
    }

    private PsiElement getElementToLook(LattePhpMethod method) {
        PsiElement nameIdentifier = method.getNameIdentifier();
        return nameIdentifier != null ? nameIdentifier : method;
    }
}
