package dev.noctud.latte.inspections;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code new Foo(...)} names a class. The lexer hands an unqualified name followed by a bracket to
 * the same PSI element a function call uses, so the inspection looked the constructor up among the
 * functions and reported {@code {var $d = new DateTimeImmutable('2026-01-31')}} as calling a
 * function that does not exist - with the class sitting in the index all along.
 *
 * <p>A qualified name takes the other route through the parser and arrives as a class reference,
 * which is why {@code new App\Model\Article} was never reported. Both spellings are here, so the
 * two halves of the same construct cannot drift apart.
 */
public class ConstructorCallTest extends BasePlatformTestCase {

    private static final String ARTICLE_PHP =
        "<?php declare(strict_types=1);\n"
            + "\n"
            + "namespace App\\Model;\n"
            + "\n"
            + "final class Article\n"
            + "{\n"
            + "    public function __construct(public string $title = '') {}\n"
            + "}\n";

    private static final String GLOBAL_PHP =
        "<?php\n"
            + "\n"
            + "class DateTimeImmutable\n"
            + "{\n"
            + "    public function __construct(string $datetime = 'now') {}\n"
            + "}\n"
            + "\n"
            + "function strtoupper(string $string): string {}\n"
            + "\n"
            + "/** @deprecated */\n"
            + "class OldGlobal\n"
            + "{\n"
            + "}\n"
            + "\n"
            + "/** @internal */\n"
            + "class InternalGlobal\n"
            + "{\n"
            + "}\n";

    private static final String OLD_THING_PHP =
        "<?php declare(strict_types=1);\n"
            + "\n"
            + "namespace App\\Model;\n"
            + "\n"
            + "/** @deprecated */\n"
            + "final class OldThing\n"
            + "{\n"
            + "}\n";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("app/Model/Article.php", ARTICLE_PHP);
        myFixture.addFileToProject("stubs/global.php", GLOBAL_PHP);
        myFixture.addFileToProject("app/Model/OldThing.php", OLD_THING_PHP);
        myFixture.enableInspections(new MethodUsagesInspection(), new ClassUsagesInspection());
    }

    public void testAnUnqualifiedConstructorIsNotAFunction() {
        assertEquals(List.of(), problemsIn("{var $d = new DateTimeImmutable('2026-01-31')}\n"));
    }

    public void testAQualifiedConstructorIsNotAFunctionEither() {
        assertEquals(List.of(), problemsIn("{var $a = new App\\Model\\Article}\n"));
        assertEquals(List.of(), problemsIn("{var $a = new \\App\\Model\\Article('title')}\n"));
    }

    /**
     * The class not being there still has to be reported, or the fix would have traded a false
     * report for a missing one.
     */
    public void testAConstructorOfAClassThatIsNotThereIsReported() {
        assertEquals(
            List.of("WARNING:Undefined class '\\NoSuchClass'"),
            problemsIn("{var $x = new NoSuchClass('a')}\n")
        );
    }

    public void testAQualifiedConstructorOfAClassThatIsNotThereIsReported() {
        assertEquals(
            List.of("WARNING:Undefined class '\\App\\Model\\NoSuchClass'"),
            problemsIn("{var $x = new App\\Model\\NoSuchClass}\n")
        );
    }

    /** Both spellings say the same about a deprecated or internal class. */
    public void testADeprecatedOrInternalClassIsReportedInEitherSpelling() {
        assertTrue(endsWithOne(problemsIn("{var $o = new App\\Model\\OldThing}\n"), "Used class '\\App\\Model\\OldThing' is marked as deprecated"));
        assertTrue(endsWithOne(problemsIn("{var $o = new OldGlobal()}\n"), "Used class '\\OldGlobal' is marked as deprecated"));
        assertTrue(endsWithOne(problemsIn("{var $o = new InternalGlobal()}\n"), "Used class '\\InternalGlobal' is marked as internal"));
    }

    private static boolean endsWithOne(List<String> problems, String description) {
        return problems.size() == 1 && problems.get(0).endsWith(":" + description);
    }

    /**
     * A plain call is still looked up among the functions, so the fix cannot have been made by
     * giving up on function calls altogether.
     */
    public void testAPlainCallIsStillAFunctionCall() {
        assertEquals(List.of(), problemsIn("{strtoupper('a')}\n"));
        assertEquals(
            List.of("WARNING:Function 'noSuchFunction' not found"),
            problemsIn("{noSuchFunction('a')}\n")
        );
    }

    private List<String> problemsIn(String template) {
        myFixture.configureByText("constructor.latte", template);
        List<String> problems = new ArrayList<>();
        for (HighlightInfo info : myFixture.doHighlighting()) {
            if (info.getDescription() != null) {
                problems.add(info.getSeverity().getName() + ":" + info.getDescription());
            }
        }
        return problems;
    }
}
