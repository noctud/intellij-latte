package dev.noctud.latte.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.icons.LatteIcons;
import dev.noctud.latte.settings.LatteSettings;
import dev.noctud.latte.utils.LatteIdeHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LatteSettingsForm implements Configurable {
    private @Nullable JPanel panel1;
    private @Nullable JButton buttonHelp;
    private @Nullable JLabel logoLabel;
    private @Nullable JCheckBox enableNetteCheckBox;
    private @Nullable JCheckBox enableNetteFormsTagsCheckBox;
    private @Nullable JCheckBox enableLatteTagsAndCheckBox;

    private final Project project;
    private boolean changed = false;

    public LatteSettingsForm(Project project) {
        this.project = project;

        if (logoLabel != null) {
            logoLabel.setIcon(LatteIcons.LOGO);
        }

        if (buttonHelp != null) {
            buttonHelp.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    LatteIdeHelper.openUrl(LatteConfiguration.LATTE_HELP_URL + "en/");
                }
            });
        }

        if (enableNetteCheckBox != null) {
            enableNetteCheckBox.setSelected(getSettings().enableNette);
            enableNetteCheckBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    LatteSettingsForm.this.changed = true;
                }
            });
        }

        if (enableNetteFormsTagsCheckBox != null) {
            enableNetteFormsTagsCheckBox.setSelected(getSettings().enableNetteForms);
            enableNetteFormsTagsCheckBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    LatteSettingsForm.this.changed = true;
                }
            });
        }

        if (enableLatteTagsAndCheckBox != null) {
            enableLatteTagsAndCheckBox.setEnabled(false);
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (enableNetteCheckBox == null || enableNetteFormsTagsCheckBox == null) {
            return;
        }

        getSettings().enableNette = enableNetteCheckBox.isSelected();
        getSettings().enableNetteForms = enableNetteFormsTagsCheckBox.isSelected();

        this.changed = false;
    }

    private LatteSettings getSettings() {
        return LatteSettings.getInstance(this.project);
    }

    @Override
    public void disposeUIResources() {

    }

}
