package dev.noctud.latte.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.inspections.utils.LatteInspectionInfo;
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

public class ModifierNotAllowedInspection extends BaseLocalInspectionTool {


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
        addInspections(manager, problems, checkFile(file), isOnTheFly);
        return problems.toArray(new ProblemDescriptor[0]);
    }

    @NotNull
    List<LatteInspectionInfo> checkFile(@NotNull final PsiFile file) {
        final List<LatteInspectionInfo> problems = new ArrayList<>();
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof LatteMacroTag) {
                    checkClassicMacro((LatteMacroTag) element, problems);

                } else {
                    super.visitElement(element);
                }
            }
        });
        return problems;
    }

    private static void checkClassicMacro(
        LatteMacroTag macroTag,
        @NotNull List<LatteInspectionInfo> problems
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
                    problems.add(LatteInspectionInfo.strictError(element, "Filters are not allowed here"));

                } else {
                    super.visitElement(element);
                }
            }
        });
    }
}
