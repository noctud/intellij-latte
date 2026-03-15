package dev.noctud.latte.indexes.stubs.types;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.lang.LighterASTTokenNode;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.CharTable;
import dev.noctud.latte.indexes.extensions.LattePhpPropertyIndex;
import dev.noctud.latte.indexes.stubs.LattePhpPropertyStub;
import dev.noctud.latte.indexes.stubs.impl.LattePhpPropertyStubImpl;
import dev.noctud.latte.parser.LatteElementTypes;
import dev.noctud.latte.psi.LattePhpProperty;
import dev.noctud.latte.psi.LatteTypes;
import dev.noctud.latte.psi.impl.LattePhpPropertyImpl;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LattePhpPropertyStubType extends LattePhpStubType<LattePhpPropertyStub, LattePhpProperty> {
    public LattePhpPropertyStubType(String debugName) {
        super(debugName, LatteElementTypes.LANG);
    }

    @Override
    public LattePhpProperty createPsi(@NotNull final LattePhpPropertyStub stub) {
        return new LattePhpPropertyImpl(stub, this);
    }

    @Override
    @NotNull
    public LattePhpPropertyStub createStub(@NotNull final LattePhpProperty psi, final StubElement parentStub) {
        return new LattePhpPropertyStubImpl(parentStub, psi.getPropertyName());
    }

    @Override
    @NotNull
    public String getExternalId() {
        return "latte.phpProperty.prop";
    }

    @Override
    public void serialize(@NotNull final LattePhpPropertyStub stub, @NotNull final StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getPropertyName());
    }

    @Override
    @NotNull
    public LattePhpPropertyStub deserialize(@NotNull final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        String name = dataStream.readNameString();
        return new LattePhpPropertyStubImpl(parentStub, name);
    }

    @Override
    public void indexStub(@NotNull final LattePhpPropertyStub stub, @NotNull final IndexSink sink) {
        sink.occurrence(LattePhpPropertyIndex.KEY, stub.getPropertyName());
    }

    @NotNull
    @Override
    public LattePhpPropertyStub createStub(@NotNull LighterAST tree, @NotNull LighterASTNode node, @NotNull StubElement parentStub) {
        LighterASTNode keyNode = LightTreeUtil.firstChildOfType(tree, node, TokenSet.create(LatteTypes.T_PHP_IDENTIFIER));
        String key = intern(tree.getCharTable(), keyNode);
        return new LattePhpPropertyStubImpl(parentStub, key);
    }

    public static String intern(@NotNull CharTable table, LighterASTNode node) {
        assert node instanceof LighterASTTokenNode : node;
        return table.intern(((LighterASTTokenNode) node).getText()).toString();
    }
}
