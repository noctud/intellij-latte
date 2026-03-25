package dev.noctud.latte.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import dev.noctud.latte.psi.LatteFile;
import dev.noctud.latte.psi.elements.LattePhpVariableElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LattePhpCachedVariable {

    private final int position;
    private final @NotNull LattePhpVariableElement element;
    private @Nullable PsiElement context = null;
    private boolean contextInitialized = false;
    private boolean definition = false;
    private @Nullable List<LattePhpVariableElement> definitions = null;
    private boolean definitionInitialized = false;
    private boolean definitionInForeachInitialized = false;
    private boolean definitionInForeach = false;
    private @Nullable String parentMacroName = null;
    private LattePhpStatement statement = null;
    private boolean statementInitialized = false;
    private boolean probablyUndefined = false;
    private boolean nextElementInitialized = false;
    private @Nullable PsiElement nextElement = null;

    public LattePhpCachedVariable(int position, @NotNull LattePhpVariableElement element) {
        this.position = position;
        this.element = element;
    }

    public static @NotNull LattePhpCachedVariable fromElement(@NotNull LattePhpVariable element) {
        return new LattePhpCachedVariable(0, element);
    }

    public boolean isProbablyUndefined() {
        return probablyUndefined;
    }

    public void setProbablyUndefined(boolean probablyUndefined) {
        this.probablyUndefined = probablyUndefined;
    }

    public int getPosition() {
        return position;
    }

    public @NotNull LattePhpVariableElement getElement() {
        return element;
    }

    public boolean matchElement(LattePhpCachedVariable cachedElement) {
        return element == cachedElement.element;
    }

    public String getVariableName() {
        // name must be cached in variable element
        return element.getVariableName();
    }

    public @NotNull Project getProject() {
        // project must be cached in variable element
        return element.getProject();
    }

    public @Nullable PsiElement getVariableContext() {
        if (!contextInitialized) {
            context = findVariableContext(element);
            contextInitialized = true;
        }
        return context;
    }

    public @NotNull List<LattePhpVariableElement> getVariableDefinitions() {
        if (isDefinition()) {
            return Collections.emptyList();
        }

        if (definitions == null) {
            definitions = new ArrayList<>();
            for (LattePhpCachedVariable variable : element.getVariableDefinition()) {
                definitions.add(variable.getElement());
            }
        }
        return definitions;
    }

    public boolean isDefinition() {
        if (!definitionInitialized) {
            definition = isVariableDefinition();
            definitionInitialized = true;
        }
        return definition;
    }

    public String getParentMacroName() {
        if (parentMacroName == null) {
            parentMacroName = LatteUtil.getParentMacroName(element);
        }
        return parentMacroName;
    }

    public boolean isVarTypeDefinition() {
        return LatteTagsUtil.Type.VAR_TYPE.getTagName().equals(getParentMacroName());
    }

    public boolean isCaptureDefinition() {
        return LatteTagsUtil.Type.CAPTURE.getTagName().equals(getParentMacroName());
    }

    public boolean isBlockDefineVarDefinition() {
        return LatteTagsUtil.Type.DEFINE.getTagName().equals(getParentMacroName());
    }

    public boolean isVarDefinition() {
        return LatteTagsUtil.Type.VAR.getTagName().equals(getParentMacroName())
            || LatteTagsUtil.Type.DEFAULT.getTagName().equals(getParentMacroName());
    }

    public boolean isParametersDefinition() {
        return LatteTagsUtil.Type.PARAMETERS.getTagName().equals(getParentMacroName());
    }

    public boolean isPhpArrayVarDefinition() {
        return element.getParent() instanceof LattePhpArrayOfVariables
            && (LatteTagsUtil.Type.PHP.getTagName().equals(getParentMacroName())
            || LatteTagsUtil.Type.DO.getTagName().equals(getParentMacroName()));
    }

    public boolean isDefinitionInForeach() {
        if (!definitionInForeachInitialized) {
            definitionInForeach = isDefinitionInForeach(this.getElement());
            definitionInForeachInitialized = true;
        }
        return definitionInForeach;
    }

    public boolean isFunctionParameter() {
        return isFunctionParameterDefinition();
    }

    public boolean isNextDefinitionOperator() {
        return isNextDefinitionOperator(element);
    }

    public @Nullable LattePhpStatement getNextStatement() {
        return getNextDefinedElement() instanceof LattePhpStatement ? (LattePhpStatement) getNextDefinedElement() : null;
    }

    public @Nullable PsiElement getNextDefinedElement() {
        if (!nextElementInitialized) {
            PsiElement element = getNextElement(this.getElement());
            if (element == null || element.getNode().getElementType() != LatteTypes.T_PHP_DEFINITION_OPERATOR) {
                nextElement = null;
            } else {
                nextElement = PsiTreeUtil.skipWhitespacesAndCommentsForward(element);
            }
            nextElementInitialized = true;
        }
        return nextElement;
    }

    public @Nullable LattePhpStatement getPhpStatement() {
        if (!statementInitialized) {
            statement = PsiTreeUtil.getParentOfType(element, LattePhpStatement.class);
            statementInitialized = true;
        }
        return statement;

    }

    private boolean isVariableDefinition() {
        if (isVarTypeDefinition() || isCaptureDefinition() || isBlockDefineVarDefinition() || isParametersDefinition()) {
            return true;
        }

        if (isVarDefinition() || isPhpArrayVarDefinition()) {
            if (isNextDefinitionOperator()) {
                return true;
            }
        }

        PsiElement parent = element.getParent();
        if (parent == null) {
            return false;
        }

        if (parent.getNode().getElementType() == LatteTypes.PHP_ARRAY_OF_VARIABLES) {
            if (isNextDefinitionOperator(parent)) {
                return true;
            }
        }

        if (isDefinitionInForeach()) {
            return true;
        }

        if (isDefinitionInFor()) {
            return true;
        }

        return isFunctionParameterDefinition();
    }

    private boolean isDefinitionInFor() {
        return LatteUtil.matchParentMacroName(element, LatteTagsUtil.Type.FOR.getTagName()) && isNextDefinitionOperator();
    }

    private boolean isFunctionParameterDefinition() {
        LattePhpInBrackets brackets = PsiTreeUtil.getParentOfType(element, LattePhpInBrackets.class);
        if (brackets == null) {
            return false;
        }

        PsiElement leftBrace = brackets.getFirstChild();
        if (leftBrace == null) {
            return false;
        }

        PsiElement prev = PsiTreeUtil.prevVisibleLeaf(leftBrace);
        if (prev == null || prev.getNode().getElementType() != LatteTypes.T_PHP_KEYWORD) {
            return false;
        }

        String text = prev.getText();
        return text.equals("function") || text.equals("fn");
    }

    /**
     * Checks if two elements within the same context are in different branches
     * of an if/else/elseif structure (separated by {else} or {elseif}).
     */
    public static boolean areInDifferentBranches(@NotNull PsiElement element1, @NotNull PsiElement element2, @NotNull PsiElement context) {
        if (!(context instanceof LattePairMacro)) {
            return false;
        }

        int offset1 = Math.min(element1.getTextOffset(), element2.getTextOffset());
        int offset2 = Math.max(element1.getTextOffset(), element2.getTextOffset());

        for (PsiElement child = context.getFirstChild(); child != null; child = child.getNextSibling()) {
            int childOffset = child.getTextOffset();
            if (childOffset <= offset1 || childOffset >= offset2) {
                continue;
            }

            if (child instanceof LatteUnpairedMacro) {
                String name = ((LatteUnpairedMacro) child).getMacroOpenTag().getMacroName();
                if ("else".equals(name) || "elseif".equals(name) || "elseifset".equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if definitions in child contexts cover all branches of an if/else,
     * meaning the variable is always defined after the if/else block.
     * Requires a final {else} branch and at least one definition per branch.
     */
    public static boolean areDefinitionsInAllBranches(@NotNull List<LattePhpCachedVariable> definitions) {
        if (definitions.isEmpty()) {
            return false;
        }

        PsiElement firstContext = definitions.get(0).getVariableContext();
        if (!(firstContext instanceof LattePairMacro)) {
            return false;
        }

        for (LattePhpCachedVariable def : definitions) {
            if (def.getVariableContext() != firstContext) {
                return false;
            }
        }

        LattePairMacro pairMacro = (LattePairMacro) firstContext;
        LatteMacroTag openTag = pairMacro.getMacroOpenTag();
        if (openTag == null) {
            return false;
        }

        String macroName = openTag.getMacroName();
        if (!"if".equals(macroName) && !"ifset".equals(macroName) && !"ifchanged".equals(macroName)) {
            return false;
        }

        // Collect branch separator offsets and check if last separator is {else}
        List<Integer> branchStartOffsets = new ArrayList<>();
        branchStartOffsets.add(pairMacro.getTextOffset());
        boolean lastBranchIsElse = false;

        for (PsiElement child = pairMacro.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof LatteUnpairedMacro) {
                String name = ((LatteUnpairedMacro) child).getMacroOpenTag().getMacroName();
                if ("else".equals(name)) {
                    branchStartOffsets.add(child.getTextOffset());
                    lastBranchIsElse = true;
                } else if ("elseif".equals(name) || "elseifset".equals(name)) {
                    branchStartOffsets.add(child.getTextOffset());
                    lastBranchIsElse = false;
                }
            }
        }

        // Must have a branch separator and the last branch must be {else}
        if (branchStartOffsets.size() < 2 || !lastBranchIsElse) {
            return false;
        }

        // Check each branch has at least one definition
        boolean[] covered = new boolean[branchStartOffsets.size()];
        for (LattePhpCachedVariable def : definitions) {
            int defOffset = def.getPosition();
            int branchIndex = 0;
            for (int i = 1; i < branchStartOffsets.size(); i++) {
                if (defOffset > branchStartOffsets.get(i)) {
                    branchIndex = i;
                }
            }
            covered[branchIndex] = true;
        }

        for (boolean c : covered) {
            if (!c) {
                return false;
            }
        }

        return true;
    }

    public static boolean isSameOrParentContext(@Nullable PsiElement check, @Nullable PsiElement probablySameOrParent) {
        if (check == probablySameOrParent) {
            return true;
        } else if (probablySameOrParent == null) {
            return false;
        }
        PsiElement parent = probablySameOrParent.getParent();
        if (parent == null) {
            return false;
        }
        PsiElement parentContext = findVariableContext(parent);
        return isSameOrParentContext(check, parentContext);
    }

    public static @Nullable PsiElement findVariableContext(@NotNull PsiElement element) {
        if (element instanceof LatteFile) {
            return element;
        }
        // If element itself is a context-creating pair macro, return it directly.
        // getParentOfType below uses strict mode which skips the element itself,
        // so without this check, context pair macros are missed when walked into
        // from isSameOrParentContext.
        if (element instanceof LattePairMacro) {
            LatteMacroTag openTag = ((LattePairMacro) element).getMacroOpenTag();
            if (openTag != null && LatteTagsUtil.isContextTag(openTag.getMacroName())) {
                return element;
            }
        } else if (element instanceof LatteHtmlPairTag) {
            LatteHtmlOpenTag openTag = ((LatteHtmlPairTag) element).getHtmlOpenTag();
            if (openTag.getHtmlTagContent() != null) {
                for (LatteNetteAttr netteAttr : openTag.getHtmlTagContent().getNetteAttrList()) {
                    if (LatteTagsUtil.isContextNetteAttribute(netteAttr.getAttrName().getText())) {
                        return element;
                    }
                }
            }
        }
        if (element instanceof LattePhpVariable && (!((LattePhpVariable) element).isDefinition() && !LatteUtil.matchParentMacroName(element, LatteTagsUtil.Type.FOR.getTagName()))) {
            PsiElement mainOpenTag = PsiTreeUtil.getParentOfType(element, LatteMacroOpenTag.class, LatteHtmlOpenTag.class);
            if (mainOpenTag != null) {
                element = mainOpenTag.getParent();
            }
        }

        PsiElement context = PsiTreeUtil.getParentOfType(element, LattePairMacro.class, LatteHtmlPairTag.class, LatteFile.class);
        if (context instanceof LattePairMacro) {
            LatteMacroTag openTag = ((LattePairMacro) context).getMacroOpenTag();
            if (openTag == null || !LatteTagsUtil.isContextTag(openTag.getMacroName())) {
                return findVariableContext(context);
            }
            return context;

        } else if (context instanceof LatteHtmlPairTag) {
            LatteHtmlOpenTag openTag = ((LatteHtmlPairTag) context).getHtmlOpenTag();
            if (openTag.getHtmlTagContent() == null) {
                return findVariableContext(context);
            }
            for (LatteNetteAttr netteAttr : openTag.getHtmlTagContent().getNetteAttrList()) {
                if (LatteTagsUtil.isContextNetteAttribute(netteAttr.getAttrName().getText())) {
                    return context;
                }
            }
            return findVariableContext(context);
        }
        return context instanceof LatteFile ? context : PsiTreeUtil.getParentOfType(element, LatteFile.class);
    }

    public static @Nullable PsiElement getNextElement(@NotNull PsiElement element) {
        LattePhpStatement statement = PsiTreeUtil.getParentOfType(element, LattePhpStatement.class);
        if (statement == null) {
            LattePhpTypedArguments typedArguments = PsiTreeUtil.getParentOfType(element, LattePhpTypedArguments.class);
            return typedArguments != null ? PsiTreeUtil.skipWhitespacesAndCommentsForward(typedArguments) : null;
        }
        return PsiTreeUtil.skipWhitespacesAndCommentsForward(statement);
    }

    public static boolean isNextDefinitionOperator(@NotNull PsiElement element) {
        PsiElement nextElement = getNextElement(element);
        return nextElement != null && nextElement.getNode().getElementType() == LatteTypes.T_PHP_DEFINITION_OPERATOR;
    }

    private static boolean isDefinitionInForeach(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent != null && parent.getNode().getElementType() == LatteTypes.PHP_FOREACH) {
            PsiElement prevElement = PsiTreeUtil.skipWhitespacesAndCommentsBackward(element);
            if (prevElement != null && prevElement.getNode().getElementType() == LatteTypes.T_PHP_REFERENCE_OPERATOR) {
                prevElement = PsiTreeUtil.skipWhitespacesAndCommentsBackward(prevElement);
            }
            IElementType type = prevElement != null ? prevElement.getNode().getElementType() : null;
            return type == LatteTypes.T_PHP_AS || type == LatteTypes.T_PHP_DOUBLE_ARROW;

        } else if (parent != null && parent.getNode().getElementType() == LatteTypes.PHP_ARRAY_OF_VARIABLES) {
            return isDefinitionInForeach(parent);
        }
        return false;
    }
}
