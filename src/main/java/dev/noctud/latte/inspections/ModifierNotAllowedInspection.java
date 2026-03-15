package dev.noctud.latte.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.psi.*;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.LatteMacroContent;
import dev.noctud.latte.psi.LatteMacroModifier;
import dev.noctud.latte.psi.LatteMacroTag;
import dev.noctud.latte.settings.LatteTagSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModifierNotAllowedInspection extends LocalInspectionTool {


    @NotNull
    @Override
    public String getShortName() {
        return "ModifierNotAllowed";
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
                if (element instanceof LatteMacroTag) {
                    checkClassicMacro((LatteMacroTag) element, problems, manager, isOnTheFly);

                } else {
                    super.visitElement(element);
                }
            }
        });
        return problems.toArray(new ProblemDescriptor[0]);
    }

    private static void checkClassicMacro(
        LatteMacroTag macroTag,
        @NotNull List<ProblemDescriptor> problems,
        @NotNull final InspectionManager manager,
        final boolean isOnTheFly
    ) {
        String name = macroTag.getMacroName();
        LatteTagSettings macro = LatteConfiguration.getInstance(macroTag.getProject()).getTag(name);
        if (macro == null || macro.isAllowedModifiers()) {
            return;
        }

        LatteMacroContent content = macroTag.getMacroContent();
        if (content == null) {
            return;
        }

        content.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof LatteMacroModifier && !((LatteMacroModifier) element).isVariableModifier()) {
                    String description = "Filters are not allowed here";
                    ProblemDescriptor problem = manager.createProblemDescriptor(element, description, true, ProblemHighlightType.GENERIC_ERROR, isOnTheFly);
                    problems.add(problem);

                } else {
                    super.visitElement(element);
                }
            }
        });
    }
}
