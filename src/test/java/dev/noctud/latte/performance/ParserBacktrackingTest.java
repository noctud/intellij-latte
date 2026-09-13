package dev.noctud.latte.performance;

import com.intellij.psi.PsiFile;
import dev.noctud.latte.BasePsiParsingTestCase;
import dev.noctud.latte.config.LatteConfiguration;
import dev.noctud.latte.settings.LatteSettings;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * Asserts the <b>shape of the cost curve</b> of parsing nested brackets inside
 * a tag, rather than any absolute time.
 *
 * <h2>Why a ratio and not a budget</h2>
 *
 * An absolute budget can only tell "it finished" from "it did not". It cannot
 * tell a linear parser from an exponential one, because both finish on a small
 * enough input - and the input that freezes an editor is small: three levels of
 * nesting beyond the last one that passed is the difference between instant and
 * a hang. A budget therefore has to be set at the exact depth the defect
 * happens to reach today, and it goes stale the moment a machine gets faster.
 *
 * So this test parses the <b>same shape at two depths</b>, one double the
 * other, in one run on one machine, and asserts the ratio between them. What
 * that ratio is worth:
 *
 * <pre>
 *   parser is linear in depth      doubling costs about 2x
 *   parser is quadratic            doubling costs about 4x
 *   parser is cubic                doubling costs about 8x
 *   parser doubles per level       doubling depth 7 -&gt; 14 costs 2^7 = 128x
 * </pre>
 *
 * {@link #MAX_RATIO} is therefore set at 8: everything up to a cubic parser
 * passes, and the exponential blowup this test was written for misses it by
 * more than an order of magnitude. Nothing about the number depends on how fast
 * the machine is, because both halves of every ratio are measured on the same
 * machine in the same run, interleaved.
 *
 * <h2>The three shapes</h2>
 *
 * <ol>
 *   <li><b>unclosed brackets</b>, {@code &#123;= [[[...} - what holding the
 *       bracket key down produces, and the cheapest way to reach a large depth;
 *   <li><b>a balanced array literal</b>, {@code &#123;= [1, [1, ...]]} - valid
 *       Latte, to show the curve is a property of the grammar and not of broken
 *       input;
 *   <li><b>the same literal with keys</b>, {@code &#123;= [0 =&gt; [0 =&gt; ...]]} -
 *       the control. It was linear even while the other two were exponential,
 *       because back then {@code phpArrayItem} tried a key first and only threw
 *       the parsed subtree away when no arrow followed - which is exactly what
 *       does not happen when there is an arrow. Keeping it in the table is what
 *       makes a failure diagnostic: if (1) and (2) fail while (3) passes, the
 *       array item rule is parsing its subtree twice again; if all three fail,
 *       something more general regressed.
 * </ol>
 *
 * <h2>How much room there is today</h2>
 *
 * <pre>
 *   unclosed brackets       depth  7 -&gt; 14     0.27 ms -&gt; 0.32 ms    1.2x
 *   balanced array          depth  8 -&gt; 16     0.27 ms -&gt; 0.32 ms    1.2x
 *   keyed array (control)   depth  8 -&gt; 16     0.21 ms -&gt; 0.30 ms    1.4x
 * </pre>
 *
 * All three are far under the limit at these depths. That says nothing about
 * much deeper nesting: both array shapes have been measured to jump by more
 * than an order of magnitude somewhere between 96 and 128 levels, which this
 * test does not reach. Every ratio is printed on every run, so the margin
 * cannot erode unseen.
 */
public class ParserBacktrackingTest extends BasePsiParsingTestCase {

    /**
     * Rounds parsed and thrown away before measuring. Only the shallow input of
     * each shape is warmed: it runs the very same parser methods as the deep
     * one, so it takes the interpreter and the first JIT tiers out of both
     * halves of every ratio, without paying the deep input's cost three more
     * times while the defect is still present.
     */
    private static final int WARMUP_ROUNDS = 3;

    /**
     * Measured rounds per input, interleaved - every input once per round. The
     * median is kept, so a GC pause or a busy patch of the machine has to hit
     * three of the five samples before it can move a result.
     */
    private static final int MEASURED_RUNS = 5;

    /**
     * The most a doubling of the nesting depth may cost. See the class comment:
     * a cubic parser sits exactly on this line, and doubling per level is 128x
     * at these depths.
     */
    private static final double MAX_RATIO = 8.0;

    /**
     * The most a doubling of the number of items in one tag may cost. Measured,
     * a linear parser doubles to about 1.8x here and a quadratic one to about
     * 3.8x, so 3 sits between them and belongs to neither.
     */
    private static final double MAX_WIDTH_RATIO = 3.0;

    /**
     * Below this, the difference between two medians is scheduler noise rather
     * than parser work, so it is used as the floor of every denominator. A
     * shallow input that measures 10 us must not be able to turn 40 us of
     * jitter on the deep one into a failure.
     */
    private static final double DENOMINATOR_FLOOR_MS = 0.05;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LatteConfiguration.getInstance(getProject());
        getProject().registerService(LatteSettings.class);
    }

    @Override
    protected String getTestDataPath() {
        return "";
    }

    @Test
    public void testDoublingTheNestingDepthDoesNotExplode() {
        assertRatios("backtracking", "depth", MAX_RATIO,
            "Parsing cost more than " + (long) MAX_RATIO + "x when the nesting depth doubled,"
                + " which is super-cubic growth in the depth of a bracket - the shape of a parser that\n"
                + "re-parses the same text on every level. Ratios measured in this run:\n",
            Arrays.asList(
                new Shape("unclosed brackets", "{= [[[...", 7, 14, ParserBacktrackingTest::unclosedBrackets),
                new Shape("balanced array", "{= [1, [1, ...]]}", 8, 16, ParserBacktrackingTest::balancedArray),
                new Shape("keyed array (control)", "{= [0 => [0 => ...]]}", 8, 16, ParserBacktrackingTest::keyedArray)
            ));
    }

    /**
     * The same claim on the other axis: how one tag grows <b>wider</b>, not deeper.
     *
     * Depth is bounded by the language - a tag is {@code &#123;...&#125;} and not
     * {@code &#123;&#123;&#123;...&#125;&#125;&#125;} - but width is not: an argument
     * list, a chain of filters and a body of statements are all flat lists a template
     * may make as long as it likes. Cost therefore has to be measured against the
     * number of tokens in one tag as well, and that is where it was quadratic: an
     * alternative that parses a whole expression before looking for the token that
     * would have told it apart parses the same text once per item of the list.
     *
     * {@link #MAX_WIDTH_RATIO} is 3: a linear parser doubles to about 1.8x here and a
     * quadratic one to about 3.8x, so the line sits between them rather than near
     * either.
     *
     * <p>Two of the shapes have no {@code ,}, {@code ;} or {@code |} in them. Those
     * three tokens end an array key after one item, so an alternative that parses a
     * key before looking for its arrow stays linear on every shape that has one and
     * is quadratic only on the shapes without. The two controls were linear all
     * along: a flat array, and many small tags one after another, which is the same
     * width spread over tags instead of packed into one - if they fail too, something
     * more general regressed than an alternative inside a tag.
     *
     * <p><b>What the ratio does not catch, and why the printed times matter.</b> The
     * unclosed-bracket shape was a power law of about n^1.5 rather than a square, so it
     * measured 2.9x per doubling - under this limit while costing 212 ms at 256
     * brackets, against 4 ms now. A limit tightened until it caught that would sit
     * inside the noise of the shapes that are linear, which is a worse trade: a test
     * that fails on a busy machine gets ignored, and then it guards nothing at all. The
     * absolute times are printed on every run for exactly this reason - for this shape
     * they are the finding and the ratio is the floor.
     */
    @Test
    public void testDoublingTheWidthOfATagDoesNotExplode() {
        assertRatios("width", "width", MAX_WIDTH_RATIO,
            "Parsing cost more than " + (long) MAX_WIDTH_RATIO + "x when the number of items in one\n"
                + "tag doubled, which is quadratic growth in the width of a tag - the shape of a parser\n"
                + "that parses the same text again for every item. Ratios measured in this run:\n",
            Arrays.asList(
                new Shape("unclosed brackets", "{= [[[...", 128, 256, ParserBacktrackingTest::unclosedBrackets),
                new Shape("argument list", "{foo a, a, ...}", 128, 256, ParserBacktrackingTest::argumentList),
                new Shape("filter chain", "{$x|f|f|...}", 128, 256, ParserBacktrackingTest::filterChain),
                new Shape("statement body", "{php $a = 1; ...}", 128, 256, ParserBacktrackingTest::statementBody),
                new Shape("no separator", "{= $a + $a + ...}", 128, 256, ParserBacktrackingTest::sumWithoutSeparator),
                new Shape("args, no separator", "{foo a a a ...}", 128, 256, ParserBacktrackingTest::argumentsWithoutSeparator),
                new Shape("flat array (control)", "{= [1, 1, ...]}", 128, 256, ParserBacktrackingTest::flatArray),
                new Shape("many tags (control)", "{$a}{$a}...", 128, 256, ParserBacktrackingTest::manyTags)
            ));
    }

    /**
     * Measures, and when something is over the limit measures the whole thing again.
     *
     * <p>A shape that is really quadratic is over the limit in every attempt, because the growth
     * is in the parser; a busy machine is over it in one attempt and under it in the next. Taking
     * the median of five runs does not separate the two, and that is not a matter of taking more
     * of them: load slows the deep input more than the shallow one, so the ratio it produces is
     * inflated for as long as the load lasts, and every run inside that window is inflated with
     * it. A second attempt is a second window.
     *
     * <p>Measured before this: over eight runs on an otherwise busy machine the test failed three
     * times, on two different shapes, both with and without the change being reviewed at the time
     * - so what it reported was the machine, and a gate that reports the machine is one people
     * learn to ignore. The comment on the limit below already said so.
     */
    private void assertRatios(String label, String axis, double maxRatio, String message, List<Shape> shapes) {
        Map<String, String> over = measureOnce(label + " attempt 1", axis, maxRatio, shapes);
        if (over.isEmpty()) {
            return;
        }

        System.out.println("[" + label + "] over the limit on the first attempt: " + over.keySet()
            + " - measuring again, because a shape that is really quadratic is over it every time");
        Map<String, String> overAgain = measureOnce(label + " attempt 2", axis, maxRatio, shapes);

        StringBuilder failures = new StringBuilder();
        for (String line : failingInBothAttempts(over, overAgain)) {
            failures.append("  ").append(line).append('\n');
        }
        assertTrue(message + failures, failures.length() == 0);
    }

    /**
     * The shapes over the limit in both attempts, with the line from each - so a report says what
     * was measured twice, not once.
     *
     * <p>Its own test is below. Without one the retry would be a change nobody could tell from
     * "the test stopped failing", which is the failure it exists to prevent.
     */
    static List<String> failingInBothAttempts(Map<String, String> first, Map<String, String> second) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> shape : second.entrySet()) {
            if (first.containsKey(shape.getKey())) {
                lines.add(first.get(shape.getKey()));
                lines.add(shape.getValue());
            }
        }
        return lines;
    }

    /**
     * What the retry must and must not swallow. Real quadratic growth is in the parser, so it is
     * over the limit in every attempt; a busy machine is over it in one and under it in the next.
     */
    @Test
    public void testTheRetryKeepsWhatIsOverTheLimitTwiceAndDropsWhatIsOverItOnce() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("really quadratic", "attempt 1: really quadratic 12.0x");
        first.put("busy machine", "attempt 1: busy machine 4.0x");

        Map<String, String> second = new LinkedHashMap<>();
        second.put("really quadratic", "attempt 2: really quadratic 11.4x");

        assertEquals(
            "a shape over the limit twice is a finding and has to survive the retry",
            List.of("attempt 1: really quadratic 12.0x", "attempt 2: really quadratic 11.4x"),
            failingInBothAttempts(first, second)
        );

        assertEquals(
            "nothing over the limit twice means nothing to report",
            List.of(),
            failingInBothAttempts(first, new LinkedHashMap<>())
        );
    }

    /**
     * One measurement of every shape. Returns the shapes that came out over the limit, by name,
     * with the line that says so - the caller needs the names to compare two attempts and the
     * lines to report what it saw.
     */
    private Map<String, String> measureOnce(String label, String axis, double maxRatio, List<Shape> shapes) {
        List<Input> inputs = new ArrayList<>();
        for (Shape shape : shapes) {
            inputs.add(new Input(shape, shape.shallowDepth));
            inputs.add(new Input(shape, shape.deepDepth));
        }

        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            for (Input input : inputs) {
                if (input.depth == input.shape.shallowDepth) {
                    parse(input);
                }
            }
        }

        double[][] samples = new double[inputs.size()][MEASURED_RUNS];
        for (int run = 0; run < MEASURED_RUNS; run++) {
            for (int i = 0; i < inputs.size(); i++) {
                long t0 = System.nanoTime();
                parse(inputs.get(i));
                samples[i][run] = (System.nanoTime() - t0) / 1_000_000.0;
            }
        }

        Map<String, String> over = new LinkedHashMap<>();
        System.out.println("[" + label + "] runs=" + MEASURED_RUNS
            + " (+" + WARMUP_ROUNDS + " warmup rounds on the shallow input of each shape)"
            + ", limit " + (long) maxRatio + "x per doubling of " + axis);
        for (int i = 0; i < inputs.size(); i += 2) {
            Shape shape = inputs.get(i).shape;
            double shallowMs = median(samples[i]);
            double deepMs = median(samples[i + 1]);
            double ratio = deepMs / Math.max(shallowMs, DENOMINATOR_FLOOR_MS);

            String line = String.format(Locale.ROOT,
                "%-22s %-22s %s %3d %9.3f ms -> %s %3d %9.3f ms   %8.1fx",
                shape.name, shape.sketch,
                axis, shape.shallowDepth, shallowMs, axis, shape.deepDepth, deepMs, ratio);
            System.out.println("[" + label + "] " + line
                + (ratio > maxRatio ? "   OVER THE " + (long) maxRatio + "x LIMIT" : ""));
            if (ratio > maxRatio) {
                over.put(shape.name, label + ": " + line);
            }
        }
        return over;
    }

    /** {@code &#123;foo a, a, ..., a&#125;} with {@code width} arguments. */
    private static String argumentList(int width) {
        return "{foo " + "a, ".repeat(width) + "a}";
    }

    /** {@code &#123;$x|f|f|...&#125;} with {@code width} filters. */
    private static String filterChain(int width) {
        return "{$x" + "|f".repeat(width) + "}";
    }

    /** {@code &#123;php $a = 1; $a = 1; ...&#125;} with {@code width} statements. */
    private static String statementBody(int width) {
        return "{php " + "$a = 1; ".repeat(width) + "}";
    }

    /** {@code &#123;= [1, 1, ..., 1]&#125;} with {@code width} items and no nesting. */
    private static String flatArray(int width) {
        return "{= [" + "1, ".repeat(width) + "1]}";
    }

    /**
     * {@code &#123;= $a + $a + ... $a&#125;} with {@code width} operands. No {@code ,}, {@code ;}
     * or {@code |}, and those are what stop a key after one item, so a guard that relies on them
     * is not tested by the shapes above.
     */
    private static String sumWithoutSeparator(int width) {
        return "{= " + "$a + ".repeat(width) + "$a}";
    }

    /** {@code &#123;foo a a a ... a&#125;} with {@code width} arguments and no separator. */
    private static String argumentsWithoutSeparator(int width) {
        return "{foo " + "a ".repeat(width) + "a}";
    }

    /** {@code &#123;$a&#125;&#123;$a&#125;...} - many tags one after another, each of them small. */
    private static String manyTags(int width) {
        return "{$a}".repeat(width);
    }

    /** {@code &#123;= } followed by {@code depth} opening brackets and nothing else. */
    private static String unclosedBrackets(int depth) {
        StringBuilder text = new StringBuilder("{= ");
        for (int i = 0; i < depth; i++) {
            text.append('[');
        }
        return text.toString();
    }

    /** {@code &#123;= [1, [1, [1, 1]]]}} nested {@code depth} levels deep. */
    private static String balancedArray(int depth) {
        return "{= " + nest(depth, "[1, ", "]") + "}";
    }

    /** {@code &#123;= [0 =&gt; [0 =&gt; [0 =&gt; 1]]]}} nested {@code depth} levels deep. */
    private static String keyedArray(int depth) {
        return "{= " + nest(depth, "[0 => ", "]") + "}";
    }

    private static String nest(int depth, String open, String close) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            text.append(open);
        }
        text.append('1');
        for (int i = 0; i < depth; i++) {
            text.append(close);
        }
        return text.toString();
    }

    /**
     * Nothing is asserted about the resulting tree here - the parser tests own
     * correctness - but a throw has to name the input that caused it, which a
     * bare stack trace out of the parser would not.
     */
    private void parse(Input input) {
        try {
            PsiFile psiFile = createPsiFile(input.psiName, input.text);
            ensureParsed(psiFile);
        } catch (Throwable t) {
            throw new AssertionError("parsing " + input.psiName + " threw " + t, t);
        }
    }

    private static double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1
            ? sorted[middle]
            : (sorted[middle - 1] + sorted[middle]) / 2.0;
    }

    private interface Generator {
        String at(int depth);
    }

    private static final class Shape {
        final String name;
        final String sketch;
        final int shallowDepth;
        final int deepDepth;
        final Generator generator;

        Shape(String name, String sketch, int shallowDepth, int deepDepth, Generator generator) {
            this.name = name;
            this.sketch = sketch;
            this.shallowDepth = shallowDepth;
            this.deepDepth = deepDepth;
            this.generator = generator;
        }
    }

    private static final class Input {
        final Shape shape;
        final int depth;
        final String text;
        final String psiName;

        Input(Shape shape, int depth) {
            this.shape = shape;
            this.depth = depth;
            this.text = shape.generator.at(depth);
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < shape.name.length(); i++) {
                char c = shape.name.charAt(i);
                name.append(Character.isLetterOrDigit(c) ? c : '_');
            }
            this.psiName = name + "_" + depth;
        }
    }
}
