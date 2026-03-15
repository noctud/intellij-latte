package dev.noctud.latte.codeStyle;

import com.intellij.application.options.*;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.*;
import dev.noctud.latte.LatteLanguage;
import org.jetbrains.annotations.*;

public class LatteCodeStyleSettingsProvider extends CodeStyleSettingsProvider {
    @Override
    public Language getLanguage() {
        return LatteLanguage.INSTANCE;
    }

    @Override
    public CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new LatteCodeStyleSettings(settings);
    }

    @Nullable
    @Override
    public String getConfigurableDisplayName() {
        return LatteLanguage.INSTANCE.getDisplayName();
    }

    @NotNull
    public CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
            @Override
            protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
                return new LatteCodeStyleMainPanel(getCurrentSettings(), settings);
            }
        };
    }

    private static class LatteCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {
        public LatteCodeStyleMainPanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
            super(LatteLanguage.INSTANCE, currentSettings, settings);
        }
    }
}
