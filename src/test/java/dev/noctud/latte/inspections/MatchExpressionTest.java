package dev.noctud.latte.inspections;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code match (...) {...}} is a PHP 8 expression. An identifier followed by a bracket is parsed as
 * a call, so it used to be looked up among the functions and reported as one that does not exist;
 * and a match spread over several lines was reported as a syntax error at its last brace.
 *
 * <p>{@code match} is still a valid method name, so a method of that name is still checked.
 */
public class MatchExpressionTest extends BasePlatformTestCase {

    private static final String REGEX_PHP =
        "<?php\n"
            + "\n"
            + "namespace App;\n"
            + "\n"
            + "final class Regex\n"
            + "{\n"
            + "    public function match(string $s): bool {}\n"
            + "}\n";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("app/Regex.php", REGEX_PHP);
        myFixture.enableInspections(new MethodUsagesInspection());
    }

    public void testAMatchIsNotAFunction() {
        assertEquals(List.of(), problemsIn("{=match ($x) { 1 => 'a', default => 'b' }}\n"));
        assertEquals(List.of(), problemsIn("{=match($x) { 1 => 'a', default => 'b' }}\n"));
    }

    public void testAMatchOnSeveralLinesIsNoSyntaxError() {
        assertEquals(List.of(), problemsIn(
            "{=match ($x) {\n"
                + "\t1 => ('a'|upper),\n"
                + "\tdefault => 'b',\n"
                + "}}\n"
                + "<p>after</p>\n"
        ));
    }

    public void testAMethodNamedMatchIsStillAMethod() {
        assertEquals(List.of(), problemsIn("{varType App\\Regex $re}\n{$re->match('a')}\n"));
        assertEquals(
            List.of("WARNING:Method 'matches' not found for type '\\App\\Regex'"),
            problemsIn("{varType App\\Regex $re}\n{$re->matches('a')}\n")
        );
    }

    private List<String> problemsIn(String template) {
        myFixture.configureByText("match.latte", template);
        List<String> problems = new ArrayList<>();
        for (HighlightInfo info : myFixture.doHighlighting()) {
            if (info.getDescription() != null) {
                problems.add(info.getSeverity().getName() + ":" + info.getDescription());
            }
        }
        return problems;
    }
}
