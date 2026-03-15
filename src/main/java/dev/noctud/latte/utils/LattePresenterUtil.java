package dev.noctud.latte.utils;

import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import org.jetbrains.annotations.NotNull;

public class LattePresenterUtil {
    public static boolean isPresenter(@NotNull String className) {
        return className.endsWith("Presenter");
    }

    public static boolean isAction(@NotNull Method method) {
        return method.getName().startsWith("action") || method.getName().startsWith("render");
    }

    public static boolean isSignal(@NotNull Method method) {
        return method.getName().startsWith("handle");
    }

    @NotNull
    public static String signalToMethod(String signal) {
        return "handle" + StringUtil.capitalize(signal);
    }

    @NotNull
    public static String actionToMethod(@NotNull String action) {
        return "action" + StringUtil.capitalize(action);
    }

    @NotNull
    public static String methodToLink(@NotNull String methodName) {
        if (methodName.startsWith("action") || methodName.startsWith("render") || methodName.startsWith("handle")) {
            return methodName.length() > 6 ? StringUtil.decapitalize(methodName.substring(6)) : "";
        }

        return methodName;
    }

    @NotNull
    public static String presenterToLink(@NotNull String presenter) {
        String clean = presenter.substring(0, presenter.length() - 9);

        if (clean.startsWith("Abstract")) {
            return clean.substring("Abstract".length());
        }

        if (clean.startsWith("Base")) {
            return clean.substring("Base".length());
        }

        return clean;
    }

    public static boolean matchPresenterName(@NotNull String name, @NotNull String className) {
        String clean = className.substring(0, className.length() - "Presenter".length());
        if (name.equals(clean)) {
            return true;
        }

        if (!name.startsWith("Abstract") && clean.startsWith("Abstract")) {
            return name.equals(clean.substring("Abstract".length()));
        }

        if (!name.startsWith("Base") && clean.startsWith("Base")) {
            return name.equals(clean.substring("Base".length()));
        }

        return false;
    }
}
