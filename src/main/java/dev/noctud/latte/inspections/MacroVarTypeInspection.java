package dev.noctud.latte.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.inspections.utils.LatteInspectionInfo;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.*;
import dev.noctud.latte.utils.LatteTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MacroVarTypeInspection extends BaseLocalInspectionTool {

    @NotNull
    @Override
    public String getShortName() {
        return "LatteVarType";
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
                if (element instanceof LatteMacroTag && ((LatteMacroTag) element).matchMacroName("varType")) {
                    LatteMacroContent macroContent = PsiTreeUtil.findChildOfType(element, LatteMacroContent.class);
                    if (macroContent != null) {
                        List<LattePhpContent> phpContent = new ArrayList<>(macroContent.getPhpContentList());

                        if (phpContent.size() == 0) {
                            problems.add(LatteInspectionInfo.strictError(element, "Tag {varType} must have php content."));

                        } else {
                            LattePhpContent content = phpContent.get(0);
                            List<PsiElement> children = new ArrayList<>();
                            content.acceptChildren(new PsiElementVisitor() {
                                @Override
                                public void visitElement(PsiElement element) {
                                    if (!LatteTypesUtil.whitespaceTokens.contains(element.getNode().getElementType())) {
                                        children.add(element);
                                    }
                                }
                            });
                            if (children.size() != 1) {
                                problems.add(LatteInspectionInfo.strictError(element, "First value in {varType} tag must be type definition."));

                            } else if (!(children.get(0) instanceof LattePhpTypedArguments) || ((LattePhpTypedArguments) children.get(0)).getPhpFirstTypedVariable() == null) {
                                problems.add(LatteInspectionInfo.strictError(element, "Invalid value in {varType} tag. Only type definition and variable are allowed."));
                            }
                        }
                    }

                } else {
                    super.visitElement(element);
                }
            }
        });
        return problems;
    }
}
