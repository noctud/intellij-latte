package dev.noctud.latte.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.inspections.utils.LatteInspectionInfo;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.LatteMacroTag;
import dev.noctud.latte.settings.LatteTagSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeprecatedTagInspection extends BaseLocalInspectionTool {

    @NotNull
    @Override
    public String getShortName() {
        return "DeprecatedTag";
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
                    String macroName = ((LatteMacroTag) element).getMacroName();
                    LatteTagSettings macro = LatteConfiguration.getInstance(element.getProject()).getTag(macroName);
                    if (macro != null && macro.isDeprecated()) {
                        String description = macro.getDeprecatedMessage() != null && macro.getDeprecatedMessage().length() > 0
                            ? macro.getDeprecatedMessage()
                            : "Tag {" + macroName + "} is deprecated";
                        problems.add(LatteInspectionInfo.deprecated(element, description));
                    }
                } else {
                    super.visitElement(element);
                }
            }
        });

        return problems;
    }
}
