package dev.noctud.latte.utils;

import com.intellij.openapi.diagnostic.Logger;

public class LatteLogger {
    private static final Logger LOG = Logger.getInstance(LatteLogger.class);

    public static void warn(String message) {
        LOG.warn(message);
    }
}
