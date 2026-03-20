package dev.noctud.latte.inspections;

import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.inspections.utils.LatteInspectionInfo;
import dev.noctud.latte.intentions.AddCustomLatteModifier;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.LatteMacroModifier;
import dev.noctud.latte.settings.LatteFilterSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModifierDefinitionInspection extends BaseLocalInspectionTool {

    @NotNull
    @Override
    public String getShortName() {
        return "LatteModifierDefinition";
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
        if (!(file instanceof LatteFile)) {
            return null;
        }

        final List<ProblemDescriptor> problems = new ArrayList<>();
        for (LatteInspectionInfo info : checkFile(file)) {
            LocalQuickFix[] fixes = info.getFixes();
            ProblemDescriptor problem = manager.createProblemDescriptor(info.getElement(), info.getDescription(), true, info.getType(), isOnTheFly, fixes);
            problems.add(problem);
        }

        return problems.toArray(new ProblemDescriptor[0]);
    }

    @NotNull
    List<LatteInspectionInfo> checkFile(@NotNull final PsiFile file) {
        final List<LatteInspectionInfo> problems = new ArrayList<>();
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof LatteMacroModifier) {
                    String filterName = ((LatteMacroModifier) element).getModifierName();
                    LatteFilterSettings latteFilter = LatteConfiguration.getInstance(element.getProject()).getFilter(filterName);
                    if (latteFilter == null) {
                        LatteInspectionInfo info = LatteInspectionInfo.error(element, "Undefined latte filter '" + filterName + "'");
                        IntentionManager intentionManager = IntentionManager.getInstance();
                        if (intentionManager != null) {
                            LocalQuickFix addModifierFix = intentionManager.convertToFix(new AddCustomLatteModifier(filterName));
                            if (addModifierFix != null) {
                                info.addFix(addModifierFix);
                            }
                        }
                        problems.add(info);

                    } else if (((LatteMacroModifier) element).getMacroModifierPartList().size() < latteFilter.getModifierInsert().length()) {
                        String description = "Missing required filter parameters (" + latteFilter.getModifierInsert().length() + " required)";
                        problems.add(LatteInspectionInfo.warning(element, description));
                    }

                } else {
                    super.visitElement(element);
                }
            }
        });

        return problems;
    }
}
