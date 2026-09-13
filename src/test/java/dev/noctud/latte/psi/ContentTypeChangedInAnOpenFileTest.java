package dev.noctud.latte.psi;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * A template that becomes an XML one while it is open.
 *
 * <p>{@code {contentType application/xml}} decides whether the template's data language is HTML or
 * XML, and it is written in the template - so it can appear, change or go away under a file that
 * is already open. Reading it on every question looked like the way to notice, and it is what took
 * the whole pass down: the platform asks the provider for its languages and then asks a copy of it
 * for the PSI of each, and the two read the file at different moments. The original said XML, the
 * copy said HTML, and the copy had no XML root to give:
 *
 * <pre>refused to parse text with Language: XML; languages: [XML, Latte]</pre>
 *
 * <p>Found by inspecting a whole corpus in one fixture, where one file name carries every template
 * in turn - which is the same sequence as one file being edited, and the reason this is a crash in
 * the editor and not only in a measurement.
 *
 * <p>So the copy made for a commit is told the language the original answers instead of working it
 * out from its own file, and the original and the copy agree during the commit.
 */
public class ContentTypeChangedInAnOpenFileTest extends BasePlatformTestCase {

    private static final String HTML = "<div>{$a}</div>\n";
    private static final String XML = "{contentType application/xml}\n{$data|noescape}\n";

    public void testAnHtmlTemplateFollowedByAnXmlOneUnderTheSameName() {
        myFixture.configureByText("page.latte", HTML);
        myFixture.doHighlighting();
        myFixture.configureByText("page.latte", XML);
        myFixture.doHighlighting();
        assertEquals(XMLLanguage.INSTANCE, dataLanguageOf());
    }

    public void testAnXmlTemplateFollowedByAnHtmlOneUnderTheSameName() {
        myFixture.configureByText("page.latte", XML);
        myFixture.doHighlighting();
        myFixture.configureByText("page.latte", HTML);
        myFixture.doHighlighting();
        assertEquals(HTMLLanguage.INSTANCE, dataLanguageOf());
    }

    /** The shape from the editor: the tag is typed into a template that is already open. */
    public void testTheContentTypeIsTypedIntoAnOpenTemplate() {
        myFixture.configureByText("page.latte", HTML);
        myFixture.doHighlighting();

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
            myFixture.getEditor().getDocument().insertString(0, "{contentType application/xml}\n"));
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

        myFixture.doHighlighting();
        assertEquals(XMLLanguage.INSTANCE, dataLanguageOf());
    }

    /** And taken away again, which is the same move in the other direction. */
    public void testTheContentTypeIsDeletedFromAnOpenTemplate() {
        myFixture.configureByText("page.latte", XML);
        myFixture.doHighlighting();

        WriteCommandAction.runWriteCommandAction(getProject(), () ->
            myFixture.getEditor().getDocument().deleteString(0, XML.indexOf('\n') + 1));
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

        myFixture.doHighlighting();
        assertEquals(HTMLLanguage.INSTANCE, dataLanguageOf());
    }

    /**
     * The counterweight: a template still gets the data language its content type asks for, so
     * none of the above can be met by answering HTML to everything.
     */
    public void testTheContentTypeStillDecidesTheDataLanguage() {
        myFixture.configureByText("plain.latte", HTML);
        assertEquals(HTMLLanguage.INSTANCE, dataLanguageOf());

        myFixture.configureByText("feed.latte", XML);
        assertEquals(XMLLanguage.INSTANCE, dataLanguageOf());
    }

    private com.intellij.lang.Language dataLanguageOf() {
        FileViewProvider provider = myFixture.getFile().getViewProvider();
        assertTrue("expected a Latte view provider, got " + provider.getClass(),
            provider instanceof LatteFileViewProvider);
        return ((LatteFileViewProvider) provider).getTemplateDataLanguage();
    }
}
