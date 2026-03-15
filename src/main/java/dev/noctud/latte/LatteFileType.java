package dev.noctud.latte;

import com.intellij.openapi.fileTypes.FileTypeEditorHighlighterProviders;
import com.intellij.openapi.fileTypes.LanguageFileType;
import dev.noctud.latte.icons.LatteIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.syntaxHighlighter.LatteHighlighterProvider;

import javax.swing.*;

public class LatteFileType extends LanguageFileType {
    public static final LatteFileType INSTANCE = new LatteFileType();

    @SuppressWarnings("deprecation")
    private LatteFileType() {
        super(LatteLanguage.INSTANCE);

        FileTypeEditorHighlighterProviders.INSTANCE.addExplicitExtension(this, new LatteHighlighterProvider());
    }

    @NotNull
    @Override
    public String getName() {
        return "Latte";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Latte template files";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "latte";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return LatteIcons.FILE;
    }
}
