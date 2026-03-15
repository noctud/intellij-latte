package dev.noctud.latte.reference.references;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LatteControlReference extends PsiReferenceBase<PsiElement> {
    public enum SegmentType {Component, RenderMethod, Subcomponent}

    private final String text;
    private final @Nullable String previousSegment; // For Subcomponent and RenderMethod context
    private final @Nullable String mainComponent;   // For RenderMethod context: the initial component name
    private final SegmentType type;

    public LatteControlReference(@NotNull PsiElement element, TextRange rangeInElement, boolean soft, String text, @Nullable String previousSegment, @Nullable String mainComponent, SegmentType type) {
        super(element, rangeInElement, soft);
        this.text = text;
        this.previousSegment = previousSegment;
        this.mainComponent = mainComponent;
        this.type = type;
    }

    @Override
    public @Nullable PsiElement resolve() {
        LatteFile file = (LatteFile) myElement.getContainingFile();
        if (file == null) return null;

        switch (type) {
            case Component: {
                // {control cart} -> Presenter::createComponentCart()
                return file.getControlResolver().resolveComponent(text);
            }
            case Subcomponent: {
                // {control cartControl-someForm} -> in return type of createComponentCartControl() find createComponentSomeForm()
                PsiElement parent = previousSegment != null ? file.getControlResolver().resolveComponent(previousSegment) : null;
                PhpClass parentClass = resolveMethodReturnClass(parent);
                if (parentClass != null) {
                    Method m = parentClass.findMethodByName("createComponent" + StringUtils.capitalize(text));
                    if (m != null) return m;
                }
                return parentClass;
            }
            case RenderMethod: {
                // {control poll:paginator} -> in return type of createComponentPoll() find renderPaginator()
                PsiElement componentMethod = mainComponent != null ? file.getControlResolver().resolveComponent(mainComponent) : null;
                PhpClass componentClass = resolveMethodReturnClass(componentMethod);
                if (componentClass != null) {
                    Method m = componentClass.findMethodByName("render" + StringUtils.capitalize(text));
                    if (m != null) return m;
                }
                return componentClass;
            }
        }

        return null;
    }

    private @Nullable PhpClass resolveMethodReturnClass(@Nullable PsiElement element) {
        if (element instanceof Method method) {
            Collection<PhpClass> classes = method.getType().getTypes().isEmpty() ? List.of() : PhpIndex.getInstance(method.getProject()).getAnyByFQN(method.getType().getTypes().iterator().next());
            for (PhpClass c : classes) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Object @NotNull [] getVariants() {
        List<LookupElement> variants = new ArrayList<>();
        LatteFile file = (LatteFile) myElement.getContainingFile();
        if (file == null) return variants.toArray();

        if (type == SegmentType.Component) {
            for (Method method : file.getControlResolver().getComponents()) {
                if (method.getContainingClass() != null) {
                    String name = StringUtils.uncapitalize(method.getName().substring("createComponent".length()));
                    variants.add(LookupElementBuilder.create(name).withTailText(" from " + method.getContainingClass().getName()).withIcon(AllIcons.Actions.GroupByModule));
                }
            }

        } else if (type == SegmentType.RenderMethod) {
            PsiElement componentMethod = mainComponent != null ? file.getControlResolver().resolveComponent(mainComponent) : null;
            PhpClass cls = resolveMethodReturnClass(componentMethod);
            if (cls != null) {
                for (Method m : cls.getMethods()) {
                    if (m.getName().startsWith("render") && !m.getName().equals("render")) {
                        variants.add(LookupElementBuilder.create(StringUtils.uncapitalize(m.getName().substring("render".length()))).withIcon(AllIcons.Actions.Execute));
                    }
                }
            }

        } else if (type == SegmentType.Subcomponent) {
            PsiElement parent = previousSegment != null ? file.getControlResolver().resolveComponent(previousSegment) : null;
            PhpClass cls = resolveMethodReturnClass(parent);
            if (cls != null) {
                for (Method m : cls.getMethods()) {
                    if (m.getName().startsWith("createComponent")) {
                        variants.add(LookupElementBuilder.create(StringUtils.uncapitalize(m.getName().substring("createComponent".length()))).withIcon(AllIcons.Actions.Execute));
                    }
                }
            }
        }

        return variants.toArray();
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return false; // so rename won't start rename of php classes or methods
    }

    public boolean isRefTo(@NotNull PsiElement element) {
        if (element instanceof Method method) {
            String name = method.getName();
            if (type == SegmentType.Component) {
                if (!name.equals("createComponent" + StringUtils.capitalize(text))) return false;
            } else if (type == SegmentType.RenderMethod) {
                if (!name.equals("render" + StringUtils.capitalize(text))) return false;
            } else if (type == SegmentType.Subcomponent) {
                if (!name.equals("createComponent" + StringUtils.capitalize(text))) return false;
            }
        }

        return super.isReferenceTo(element);
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) {
        return getElement();
    }
}
