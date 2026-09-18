package dev.noctud.latte.settings;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/** The PHP regression suite also checks its generated output against this shared fixture. */
public class ExportedSettingsTest {
    @Test
    public void exportedDefinitionsDeserializeWithUsableMetadata() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/data/settings/exported-latte.xml")) {
            assertNotNull(input);
            LatteSettings settings = XmlSerializer.deserialize(JDOMUtil.load(input).getChild("component"), LatteSettings.class);

            assertEquals(2, settings.tagSettings.size());
            LatteTagSettings attribute = settings.tagSettings.get(0);
            assertEquals("exportAttr", attribute.getMacroName());
            assertEquals(LatteTagSettings.Type.ATTR_ONLY, attribute.getType());
            LatteTagSettings pair = settings.tagSettings.get(1);
            assertEquals("exportPair", pair.getMacroName());
            assertEquals(LatteTagSettings.Type.PAIR, pair.getType());
            assertEquals("", pair.getArgumentsInfo());
            assertEquals("", pair.getDeprecatedMessage());

            assertEquals(1, settings.filterSettings.size());
            LatteFilterSettings filter = settings.filterSettings.get(0);
            assertEquals("exportFilter", filter.getModifierName());
            assertEquals("int $count", filter.getModifierHelp());
            assertEquals(1, filter.getModifierInsert().length());
            assertEquals("", filter.getModifierDescription().trim());

            assertEquals(1, settings.functionSettings.size());
            LatteFunctionSettings function = settings.functionSettings.get(0);
            assertEquals("exportFunction", function.getFunctionName());
            assertEquals("bool", function.getFunctionReturnType());
            assertEquals("()", function.getFunctionHelp());
            assertEquals("", function.getFunctionDescription().trim());
        }
    }
}
