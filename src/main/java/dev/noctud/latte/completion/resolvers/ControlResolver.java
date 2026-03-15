package dev.noctud.latte.completion.resolvers;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.psi.LatteFile;

import java.util.ArrayList;
import java.util.List;

public class ControlResolver extends PresenterResolver {
    //private final HashMap<String, @Nullable PsiElement> cache = new HashMap<>();

    public ControlResolver(LatteFile file) {
        super(file);
    }

    public void reset() {
        //cache.clear();
    }

    public @Nullable PsiElement resolveComponent(String component) {
        String key = component;
        /*if (cache.containsKey(key)) {
            return cache.get(key);
        }*/

        PsiElement result = calculateComponent(component);
        //cache.put(key, result);
        return result;
    }

    private @Nullable PsiElement calculateComponent(String component) {
        List<PhpClass> matchingPresenters = getMatchingPresenters(null, false);

        if (matchingPresenters.isEmpty()) {
            String lowerCase = component.toLowerCase();
            if (!lowerCase.equals("form") && !lowerCase.equals("grid") && !lowerCase.equals("datagrid") && !lowerCase.equals("addform") && !lowerCase.equals("editform")) {
                // TODO: This is the lLast resort, REMOVE when refactoring is added
                matchingPresenters = getPresenters();
            } else {
                return null;
            }
        }

        for (PhpClass presenterClass : matchingPresenters) {
            Method method = findMethod(presenterClass, List.of("createComponent" + StringUtils.capitalize(component)));
            if (method != null) {
                return method;
            }
        }

        return null;
    }

    public List<Method> getComponents() {
        List<PhpClass> matchingPresenters = getMatchingPresenters(null, false);

        if (matchingPresenters.isEmpty()) {
            return new ArrayList<>();
        }

        PhpClass presenterClass = matchingPresenters.get(0);

        List<Method> components = new ArrayList<>();
        for (Method method : presenterClass.getMethods()) {
            if (method.getName().startsWith("createComponent") && !method.getName().equals("createComponent")) {
                components.add(method);
            }
        }

        return components;
    }
}
