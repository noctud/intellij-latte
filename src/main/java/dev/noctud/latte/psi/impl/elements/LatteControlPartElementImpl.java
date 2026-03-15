package dev.noctud.latte.psi.impl.elements;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.reference.references.LatteControlReference;
import dev.noctud.latte.psi.elements.LatteControlPartElement;
import dev.noctud.latte.psi.impl.LattePsiElementImpl;

import java.util.ArrayList;
import java.util.List;

public abstract class LatteControlPartElementImpl extends LattePsiElementImpl implements LatteControlPartElement {
    private @Nullable List<PsiReference> references = null;
    private @Nullable String name = null;

    public LatteControlPartElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        references = null;
        name = null;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public @NotNull String getName() {
        if (name == null) {
            name = getText();
        }

        return name;
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
        if (references == null) {
            references = new ArrayList<>();

            String myText = getName();
            if (myText.equals(":") || myText.equals("-")) {
                return PsiReference.EMPTY_ARRAY;
            }

            PsiElement parent = getParent();
            if (!(parent instanceof dev.noctud.latte.psi.LatteControl)) {
                return PsiReference.EMPTY_ARRAY;
            }

            String wholeText = parent.getText();

            // Compute my range within parent
            TextRange parentRange = parent.getTextRange();
            TextRange myRange = getTextRange();
            int myStartInParent = myRange.getStartOffset() - parentRange.getStartOffset();
            int myEndInParent = myRange.getEndOffset() - parentRange.getStartOffset();

            // Parse wholeText into tokens separated by ':' and '-', tracking ranges
            List<Token> tokens = new ArrayList<>();
            int pos = 0;
            int tokenStart = 0;
            while (pos < wholeText.length()) {
                char c = wholeText.charAt(pos);
                if (c == ':' || c == '-') {
                    if (tokenStart < pos) {
                        tokens.add(new Token(wholeText.substring(tokenStart, pos), tokenStart, pos));
                    }
                    tokens.add(new Token(String.valueOf(c), pos, pos + 1));
                    pos++;
                    tokenStart = pos;
                } else {
                    pos++;
                }
            }
            if (tokenStart < pos) {
                tokens.add(new Token(wholeText.substring(tokenStart, pos), tokenStart, pos));
            }

            // Determine colon segment index and context for my token
            int colonIndex = 0;
            boolean firstSegmentHasHyphen = false;
            String previousHyphenName = null;
            String mainComponent = null;

            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                if (t.isColon()) {
                    colonIndex++;
                    continue;
                }
                if (colonIndex == 0 && t.isHyphen()) {
                    firstSegmentHasHyphen = true;
                    previousHyphenName = null; // reset chain between hyphens
                    continue;
                }
                if (t.isHyphen()) {
                    // other segments hyphens are not relevant for now
                    continue;
                }

                // Update firstSegmentHasHyphen by peeking neighbors in first segment
                if (colonIndex == 0) {
                    // Look ahead for any '-' in first segment
                    for (int j = i + 1; j < tokens.size(); j++) {
                        Token n = tokens.get(j);
                        if (n.isColon()) break;
                        if (n.isHyphen()) {
                            firstSegmentHasHyphen = true;
                            break;
                        }
                    }
                }

                // Build main component text (text before first ':')
                if (mainComponent == null) {
                    StringBuilder sb = new StringBuilder();
                    for (Token mcTok : tokens) {
                        if (mcTok.isColon()) break;
                        sb.append(mcTok.text);
                    }
                    mainComponent = sb.toString();
                }

                // For tracking previous hyphen name in first segment
                if (colonIndex == 0 && !t.isHyphen()) {
                    // Name in first segment
                    if (myStartInParent == t.start && myEndInParent == t.end) {
                        String clean = t.text.replace("IntellijIdeaRulezzz", "");
                        if (firstSegmentHasHyphen) {
                            // Subcomponent chain
                            references.add(new LatteControlReference(this, new TextRange(0, getTextLength()), true, clean, previousHyphenName, null, LatteControlReference.SegmentType.Subcomponent));
                        } else {
                            // Single component name
                            references.add(new LatteControlReference(this, new TextRange(0, getTextLength()), true, clean, null, null, LatteControlReference.SegmentType.Component));
                        }
                        break;
                    }
                    previousHyphenName = t.text;
                    // next iteration will see this as previous
                } else {
                    // Render method part (after ':')
                    if (myStartInParent == t.start && myEndInParent == t.end) {
                        String clean = t.text.replace("IntellijIdeaRulezzz", "");
                        references.add(new LatteControlReference(this, new TextRange(0, getTextLength()), true, clean, null, mainComponent, LatteControlReference.SegmentType.RenderMethod));
                        break;
                    }
                }
            }
        }

        return references.toArray(new PsiReference[0]);
    }

    private static class Token {
        final String text;
        final int start;
        final int end;

        Token(String text, int start, int end) {
            this.text = text;
            this.start = start;
            this.end = end;
        }

        boolean isColon() {
            return ":".equals(text);
        }

        boolean isHyphen() {
            return "-".equals(text);
        }
    }
}
