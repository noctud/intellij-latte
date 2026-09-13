package dev.noctud.latte.psi;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.LightVirtualFile;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.LatteLanguage;

public class LatteFileViewProviderTest extends BasePsiParsingTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/data";
    }

    public void testDetectXmlFromContentTypeTag() {
        assertTrue(LatteFileViewProvider.detectXmlContentType("{contentType application/xml}\n<root/>"));
    }

    public void testDetectXmlWithTrailingGarbageOnFirstLine() {
        assertTrue(LatteFileViewProvider.detectXmlContentType("{contentType application/xml} {* comment *}\n"));
    }

    public void testDetectXmlAcceptsPrefixedXhtml() {
        assertTrue(LatteFileViewProvider.detectXmlContentType("{contentType application/xhtml+xml}\n"));
    }

    public void testPlainHtmlIsNotDetectedAsXml() {
        assertFalse(LatteFileViewProvider.detectXmlContentType("<html>{$var}</html>"));
    }

    public void testNonXmlContentTypeIsNotDetectedAsXml() {
        assertFalse(LatteFileViewProvider.detectXmlContentType("{contentType text/plain}\nhello"));
    }

    public void testContentTypeMustBeOnFirstLine() {
        assertFalse(LatteFileViewProvider.detectXmlContentType("<html>\n{contentType application/xml}\n</html>"));
    }

    public void testEmptyContentIsNotDetectedAsXml() {
        assertFalse(LatteFileViewProvider.detectXmlContentType(""));
    }

    public void testProviderReadsTheDataLanguageFromItsOwnContents() {
        assertSame(HTMLLanguage.INSTANCE, dataLanguageOf("<html></html>"));
    }

    /**
     * The same question the other way round. The provider used to answer HTML here whatever the
     * text said, because reading it meant VFS I/O and that was refused on EDT; it reads its own
     * contents now, so a content type in the text is seen wherever the question is asked.
     */
    public void testProviderReadsXmlFromItsOwnContents() {
        assertSame(XMLLanguage.INSTANCE, dataLanguageOf("{contentType application/xml}\n<root/>"));
    }

    private com.intellij.lang.Language dataLanguageOf(String text) {
        LightVirtualFile vf = new LightVirtualFile("test.latte", LatteLanguage.INSTANCE, text);
        vf.setCharset(CharsetToolkit.UTF8_CHARSET);
        return new LatteFileViewProvider(PsiManager.getInstance(getProject()), vf, false).getTemplateDataLanguage();
    }
}
