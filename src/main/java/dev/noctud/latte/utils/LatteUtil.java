package dev.noctud.latte.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import dev.noctud.latte.php.NettePhpType;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.*;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class LatteUtil {

    public static boolean matchParentMacroName(@NotNull PsiElement element, @NotNull String name) {
        return getParentMacroName(element).equals(name);
    }

    public static @NotNull String getParentMacroName(@NotNull PsiElement element) {
        PsiElement foundElement = PsiTreeUtil.getParentOfType(element, LatteMacroClassic.class, LatteNetteAttr.class);
        if (foundElement instanceof LatteMacroClassic) {
            return ((LatteMacroClassic) foundElement).getOpenTag().getMacroName();
        } else if (foundElement == null) {
            return "=";
        }
        String attributeName = ((LatteNetteAttr) foundElement).getAttrName().getText();
        return attributeName.replace("n:inner-", "")
            .replace("n:tag-", "")
            .replace("n:", "");
    }

    public static String getSpacesBeforeCaret(@NotNull Editor editor) {
        int startOffset = editor.getCaretModel().getOffset();
        CharSequence chars = editor.getDocument().getCharsSequence();
        if (startOffset <= 0 || chars.length() < startOffset) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        int idx = startOffset - 1;
        while (idx >= 0) {
            char letter = chars.charAt(idx);
            if (letter == '\n') {
                break;
            }
            if (letter == '\t' || letter == ' ') {
                out.append(letter);
            }
            idx--;
        }
        return out.reverse().toString();
    }

    public static boolean isStringAtCaret(@NotNull Editor editor, @NotNull String string) {
        int startOffset = editor.getCaretModel().getOffset();
        CharSequence chars = editor.getDocument().getCharsSequence();
        int len = string.length();
        if (len == 0) return true;
        if (startOffset < 0 || startOffset + len > chars.length()) return false;
        for (int i = 0; i < len; i++) {
            if (chars.charAt(startOffset + i) != string.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static NettePhpType findFirstLatteTemplateType(PsiElement element) {
        List<LattePhpClassUsage> out = new ArrayList<>();
        findLatteTemplateType(out, element);
        return out.isEmpty() ? null : out.get(0).getReturnType();
    }

    public static void findLatteTemplateType(List<LattePhpClassUsage> classes, PsiElement psiElement) {
        psiElement.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof LattePhpClassUsage && ((LattePhpClassUsage) element).isTemplateType()) {
                    classes.add((LattePhpClassUsage) element);
                } else {
                    super.visitElement(element);
                }
            }
        });
    }

    public static void findLatteMacroTemplateType(List<LatteMacroTag> classes, LatteFile file) {
        file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof LatteMacroTag && ((LatteMacroTag) element).matchMacroName("templateType")) {
                    classes.add((LatteMacroTag) element);
                } else {
                    super.visitElement(element);
                }
            }
        });
    }

    public static int getStartOffsetInFile(PsiElement psiElement) {
        return getStartOffsetInFile(psiElement, 0);
    }

    private static int getStartOffsetInFile(PsiElement psiElement, int offset) {
        PsiElement parent = psiElement.getParent();
        if (parent == null || parent instanceof LatteFile) {
            return psiElement.getStartOffsetInParent() + offset;
        }
        return getStartOffsetInFile(parent, psiElement.getStartOffsetInParent() + offset);
    }

    public static String normalizeMacroModifier(String name) {
        return name.startsWith("|") ? name.substring(1) : name;
    }

    public static String normalizeNAttrNameModifier(String name) {
        return name.startsWith("n:") ? name.substring(2) : name;
    }
}
