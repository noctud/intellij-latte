package dev.noctud.latte.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LatteTagsUtilTest {

	@Test
	public void testIsContextTag() {
		assertTrue(LatteTagsUtil.isContextTag("foreach"));
		assertTrue(LatteTagsUtil.isContextTag("for"));
		assertTrue(LatteTagsUtil.isContextTag("if"));
		assertTrue(LatteTagsUtil.isContextTag("ifset"));
		assertTrue(LatteTagsUtil.isContextTag("ifchanged"));
		assertTrue(LatteTagsUtil.isContextTag("else"));
		assertTrue(LatteTagsUtil.isContextTag("elseif"));
		assertTrue(LatteTagsUtil.isContextTag("elseifset"));
		assertTrue(LatteTagsUtil.isContextTag("while"));
		assertTrue(LatteTagsUtil.isContextTag("block"));
		assertTrue(LatteTagsUtil.isContextTag("define"));
		assertTrue(LatteTagsUtil.isContextTag("snippet"));
		assertTrue(LatteTagsUtil.isContextTag("snippetArea"));
	}

	@Test
	public void testIsContextTagNegative() {
		assertFalse(LatteTagsUtil.isContextTag("var"));
		assertFalse(LatteTagsUtil.isContextTag("include"));
		assertFalse(LatteTagsUtil.isContextTag("layout"));
		assertFalse(LatteTagsUtil.isContextTag("extends"));
		assertFalse(LatteTagsUtil.isContextTag("import"));
		assertFalse(LatteTagsUtil.isContextTag("link"));
		assertFalse(LatteTagsUtil.isContextTag("plink"));
		assertFalse(LatteTagsUtil.isContextTag("control"));
		assertFalse(LatteTagsUtil.isContextTag("form"));
		assertFalse(LatteTagsUtil.isContextTag("varType"));
		assertFalse(LatteTagsUtil.isContextTag("templateType"));
		assertFalse(LatteTagsUtil.isContextTag("default"));
		assertFalse(LatteTagsUtil.isContextTag("do"));
		assertFalse(LatteTagsUtil.isContextTag("php"));
		assertFalse(LatteTagsUtil.isContextTag("parameters"));
		assertFalse(LatteTagsUtil.isContextTag("sandbox"));
		assertFalse(LatteTagsUtil.isContextTag("embed"));
		assertFalse(LatteTagsUtil.isContextTag("capture"));
		assertFalse(LatteTagsUtil.isContextTag("nonexistent"));
		assertFalse(LatteTagsUtil.isContextTag(""));
	}

	@Test
	public void testContextNetteAttributes() {
		// n:foreach, n:tag-foreach, n:inner-foreach should all be context attributes
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:foreach"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:tag-foreach"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:inner-foreach"));

		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:if"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:tag-if"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:inner-if"));

		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:block"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:tag-block"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:inner-block"));

		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:while"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:for"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:ifset"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:ifchanged"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:else"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:elseif"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:elseifset"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:snippet"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:snippetArea"));
		assertTrue(LatteTagsUtil.isContextNetteAttribute("n:define"));

		// Non-context tags should NOT be context attributes
		assertFalse(LatteTagsUtil.isContextNetteAttribute("n:var"));
		assertFalse(LatteTagsUtil.isContextNetteAttribute("n:include"));
		assertFalse(LatteTagsUtil.isContextNetteAttribute("n:href"));
		assertFalse(LatteTagsUtil.isContextNetteAttribute("foreach"));
		assertFalse(LatteTagsUtil.isContextNetteAttribute(""));
	}
}
