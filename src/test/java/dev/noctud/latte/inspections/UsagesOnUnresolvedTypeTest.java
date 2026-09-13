package dev.noctud.latte.inspections;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * A type the plugin cannot resolve to a class is the case it cannot decide, and the four
 * inspections that look a name up on a type have to stay silent there. Reporting the name as
 * absent claims the class was read and did not have it, which is the opposite of what happened:
 * no class was found at all.
 *
 * <p>It is not a rare case. A project whose index has not finished, a template typed against a
 * class from a package that is not installed, a vendor directory that is not there - all of them
 * end here, and the plugin has nothing to say about any of them.
 *
 * <p>Every case is paired with the same name looked up on a class that is in the fixture, so
 * silence cannot be reached by an inspection that reports nothing at all.
 */
public class UsagesOnUnresolvedTypeTest extends BasePlatformTestCase {

    /** The one class the fixture has. Everything else a template names is unresolvable. */
    private static final String ARTICLE_PHP =
        "<?php declare(strict_types=1);\n"
            + "\n"
            + "namespace App\\Model;\n"
            + "\n"
            + "final class Article\n"
            + "{\n"
            + "    public const STATUS = 'published';\n"
            + "\n"
            + "    public static string $table = 'articles';\n"
            + "\n"
            + "    public string $title = '';\n"
            + "\n"
            + "    public function getTitle(): string\n"
            + "    {\n"
            + "        return $this->title;\n"
            + "    }\n"
            + "}\n";

    private static final String UNRESOLVED = "App\\Model\\NotInTheIndex";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("app/Model/Article.php", ARTICLE_PHP);
    }

    public void testMethodOnUnresolvedTypeIsNotReported() {
        myFixture.enableInspections(new MethodUsagesInspection());
        assertEquals(List.of(), problemsIn("{varType " + UNRESOLVED + " $x}\n{$x->getTitle()}\n"));
    }

    public void testMethodMissingFromAResolvedTypeIsReported() {
        myFixture.enableInspections(new MethodUsagesInspection());
        assertEquals(
            List.of("WARNING:Method 'noSuchMethod' not found for type '\\App\\Model\\Article'"),
            problemsIn("{varType App\\Model\\Article $x}\n{$x->noSuchMethod()}\n")
        );
    }

    public void testPropertyOnUnresolvedTypeIsNotReported() {
        myFixture.enableInspections(new PropertyUsagesInspection());
        assertEquals(List.of(), problemsIn("{varType " + UNRESOLVED + " $x}\n{$x->title}\n"));
    }

    public void testPropertyMissingFromAResolvedTypeIsReported() {
        myFixture.enableInspections(new PropertyUsagesInspection());
        assertEquals(
            List.of("WARNING:Property 'noSuchProperty' not found for type '\\App\\Model\\Article'"),
            problemsIn("{varType App\\Model\\Article $x}\n{$x->noSuchProperty}\n")
        );
    }

    public void testConstantOnUnresolvedTypeIsNotReported() {
        myFixture.enableInspections(new ConstantUsagesInspection());
        assertEquals(List.of(), problemsIn("{varType " + UNRESOLVED + " $x}\n{$x::STATUS}\n"));
    }

    public void testConstantMissingFromAResolvedTypeIsReported() {
        myFixture.enableInspections(new ConstantUsagesInspection());
        assertEquals(
            List.of("WARNING:Constant 'NO_SUCH_CONSTANT' not found for type '\\App\\Model\\Article'"),
            problemsIn("{varType App\\Model\\Article $x}\n{$x::NO_SUCH_CONSTANT}\n")
        );
    }

    public void testStaticPropertyOnUnresolvedTypeIsNotReported() {
        myFixture.enableInspections(new StaticPropertyUsagesInspection());
        assertEquals(List.of(), problemsIn("{varType " + UNRESOLVED + " $x}\n{$x::$table}\n"));
    }

    public void testStaticPropertyMissingFromAResolvedTypeIsReported() {
        myFixture.enableInspections(new StaticPropertyUsagesInspection());
        assertEquals(
            List.of("WARNING:Property 'noSuchTable' not found for type '\\App\\Model\\Article'"),
            problemsIn("{varType App\\Model\\Article $x}\n{$x::$noSuchTable}\n")
        );
    }

    private List<String> problemsIn(String template) {
        myFixture.configureByText("unresolved-type.latte", template);
        List<String> problems = new ArrayList<>();
        for (HighlightInfo info : myFixture.doHighlighting()) {
            if (info.getDescription() != null) {
                problems.add(info.getSeverity().getName() + ":" + info.getDescription());
            }
        }
        return problems;
    }
}
