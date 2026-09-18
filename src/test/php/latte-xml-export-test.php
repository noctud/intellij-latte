<?php declare(strict_types=1);

use Latte\Engine;
use Latte\Extension;

if ($argc !== 2 || !is_file($argv[1])) {
    throw new RuntimeException('Usage: php src/test/php/latte-xml-export-test.php /path/to/vendor/autoload.php');
}

require $argv[1];
require dirname(__DIR__, 3) . '/latte-xml-export.php';

if (!class_exists('LatteXmlExporter')) {
    throw new RuntimeException('latte-xml-export.php must define LatteXmlExporter.');
}

final class ExportFixtureExtension extends Extension
{
    public function getTags(): array
    {
        return [
            'paired' => Extension::order(function () {
                yield;
            }),
            'n:paired' => function () {
                yield;
            },
            'unpairedAttribute' => function () {
            },
            'n:unpairedAttribute' => function () {
            },
            'overridden' => function () {
                yield;
            },
            'n:overridden' => function () {
            },
            'escaped<&"\'' => function () {
            },
        ];
    }

    public function getFilters(): array
    {
        return [
            'extensionFilter' => function ($value) {
                return $value;
            },
            'overriddenFilter' => function (string $value, string $firstRequired): string {
                return 'first-' . $value;
            },
        ];
    }

    public function getFunctions(): array
    {
        return [
            'extensionFunction' => function ($value) {
                return $value;
            },
            'overriddenFunction' => function (string $value): string {
                return 'first-' . $value;
            },
        ];
    }
}

final class ExportOverrideExtension extends Extension
{
    public function getTags(): array
    {
        return [
            'overridden' => function () {
            },
        ];
    }

    public function getFilters(): array
    {
        return [
            'overriddenFilter' => function (string $value, string $lastRequired, string $secondRequired): string {
                return 'last-' . $value;
            },
        ];
    }

    public function getFunctions(): array
    {
        return [
            'overriddenFunction' => function (int $last): int {
                return $last;
            },
        ];
    }
}

final class ExportFixtureComparisonExtension extends Extension
{
    public function getTags(): array
    {
        return [
            'exportPair' => function () {
                yield;
            },
            'n:exportAttr' => function () {
            },
        ];
    }

    public function getFilters(): array
    {
        return [
            'exportFilter' => function (string $value, int $count): string {
                return $value;
            },
        ];
    }

    public function getFunctions(): array
    {
        return [
            'exportFunction' => function (): bool {
                return true;
            },
        ];
    }
}

class ContextualFunctionParent
{
}

class ContextualFunctionFixture extends ContextualFunctionParent
{
    public function selfReturn(): self
    {
        return $this;
    }

    public function nullableSelfReturn(): ?self
    {
        return null;
    }

    public function parentReturn(): parent
    {
        return new ContextualFunctionParent();
    }

    public function staticReturn(): static
    {
        return $this;
    }

    public function selfOrStringReturn(): self|string
    {
        return $this;
    }
}

final class ContextualFunctionChild extends ContextualFunctionFixture
{
}

function check(bool $condition, string $message): void
{
    if (!$condition) {
        throw new RuntimeException($message);
    }
}

function document(string $xml): DOMDocument
{
    $document = new DOMDocument();
    check($document->loadXML($xml), 'Exporter did not produce valid XML.');
    return $document;
}

function nodes(DOMDocument $document, string $query): DOMNodeList
{
    $result = (new DOMXPath($document))->query($query);
    check($result !== false, 'Invalid XPath query: ' . $query);
    return $result;
}

function one(DOMDocument $document, string $query): DOMElement
{
    $result = nodes($document, $query);
    check($result->length === 1, 'Expected exactly one match for XPath: ' . $query . ', found ' . $result->length);
    $node = $result->item(0);
    check($node instanceof DOMElement, 'Expected an element for XPath: ' . $query);
    return $node;
}

function setting(DOMDocument $document, string $listName, string $className, string $nameAttribute, string $name): DOMElement
{
    return one(
        $document,
        '/project/component[@name="LattePluginSettings"]/' . $listName . '/list/'
        . $className . '[@' . $nameAttribute . '=' . xpathLiteral($name) . ']'
    );
}

function xpathLiteral(string $value): string
{
    if (strpos($value, "'") === false) {
        return "'" . $value . "'";
    }

    if (strpos($value, '"') === false) {
        return '"' . $value . '"';
    }

    $parts = explode("'", $value);
    return "concat('" . implode("', \"'\", '", $parts) . "')";
}

function countSettings(DOMDocument $document, string $listName, string $className, string $nameAttribute, string $name): int
{
    return nodes(
        $document,
        '/project/component[@name="LattePluginSettings"]/' . $listName . '/list/'
        . $className . '[@' . $nameAttribute . '=' . xpathLiteral($name) . ']'
    )->length;
}

$engine = new Engine();
$engine->addExtension(new ExportFixtureExtension());
$engine->addExtension(new ExportOverrideExtension());
$engine->addFilter('directFilter', function ($value) {
    return $value;
});
$engine->addFilter('contextFilter', function (\Latte\Runtime\FilterInfo $info, $value, string $required, string $optional = '') {
    return $value;
});
$engine->addFilter('contextVariadicFilter', function (\Latte\Runtime\FilterInfo $info, ...$arguments) {
    return $arguments;
});
$engine->addFilter('variadicFilter', function (...$arguments) {
    return $arguments;
});
$engine->addFilter('lower', function (string $value): string {
    return 'custom-' . $value;
});
$engine->addFunction('directFunction', function ($value) {
    return $value;
});
$engine->addFunction('templateFunction', function (\Latte\Runtime\Template $template, string $argument): int {
    return 1;
});
$engine->addFunction('clamp', function (int $value): int {
    return $value;
});
$contextualFunction = new ContextualFunctionFixture();
$contextualChild = new ContextualFunctionChild();
$engine->addFunction('contextSelf', [$contextualFunction, 'selfReturn']);
$engine->addFunction('contextNullableSelf', [$contextualFunction, 'nullableSelfReturn']);
$engine->addFunction('contextParent', [$contextualFunction, 'parentReturn']);
$engine->addFunction('contextStatic', [$contextualChild, 'staticReturn']);
$engine->addFunction('contextSelfOrString', [$contextualFunction, 'selfOrStringReturn']);

$xml = LatteXmlExporter::generate($engine);
$document = document($xml);

check($document->documentElement->tagName === 'project', 'Exporter must create an IntelliJ project document.');
one($document, '/project/component[@name="LattePluginSettings"]');
one($document, '/project/component[@name="LattePluginSettings"]/customMacroSettings/list');
one($document, '/project/component[@name="LattePluginSettings"]/customModifierSettings/list');
one($document, '/project/component[@name="LattePluginSettings"]/customFunctionSettings/list');

check(setting($document, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'paired')->getAttribute('MacroType') === 'PAIR', 'A generator tag registered through Extension::order() must be a PAIR tag.');
check(setting($document, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'unpairedAttribute')->getAttribute('MacroType') === 'UNPAIRED_ATTR', 'A normal foo + n:foo tag pair must be UNPAIRED_ATTR.');
check(setting($document, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'overridden')->getAttribute('MacroType') === 'UNPAIRED_ATTR', 'Later tag registrations must override earlier generator registrations.');
check(setting($document, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'escaped<&"\'')->getAttribute('MacroType') === 'UNPAIRED', 'Custom tag was not exported.');
check(strpos($xml, 'escaped&lt;&amp;&quot;') !== false, 'Custom tag names must be XML escaped.');

check(setting($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'lower')->getAttribute('ModifierHelp') === '', 'An overridden built-in filter must be exported as a custom definition.');
check(setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'clamp')->getAttribute('FunctionReturnType') === 'int', 'An overridden built-in function must be exported as a custom definition.');

$filter = setting($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'directFilter');
check($filter->hasAttribute('ModifierDescription') && $filter->getAttribute('ModifierDescription') === '', 'Filters must explicitly write empty ModifierDescription metadata.');
check($filter->hasAttribute('ModifierDoubleInsert') && $filter->getAttribute('ModifierDoubleInsert') === '', 'Filters must explicitly write empty ModifierDoubleInsert metadata.');
check(countSettings($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'overriddenFilter') === 1, 'Later filter registrations must replace earlier registrations.');
$overriddenFilter = setting($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'overriddenFilter');
check($overriddenFilter->getAttribute('ModifierHelp') === 'string $lastRequired, string $secondRequired', 'Filter metadata must come from the later registration.');
check($overriddenFilter->getAttribute('ModifierDoubleInsert') === '::', 'Later filter registration must replace the required argument count.');
$contextFilter = setting($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'contextFilter');
check($contextFilter->getAttribute('ModifierHelp') === 'string $required, [string $optional]', 'FilterInfo and the filtered value must be omitted from filter help.');
check($contextFilter->getAttribute('ModifierDoubleInsert') === ':', 'Only required filter arguments must produce insertion colons.');
$contextVariadicFilter = setting($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'contextVariadicFilter');
check($contextVariadicFilter->getAttribute('ModifierHelp') === '...$arguments', 'A variadic parameter after FilterInfo must remain in filter help.');
check($contextVariadicFilter->getAttribute('ModifierDoubleInsert') === '', 'A variadic parameter after FilterInfo must not produce insertion colons.');
$variadicFilter = setting($document, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'variadicFilter');
check($variadicFilter->getAttribute('ModifierHelp') === '...$arguments', 'A variadic-only filter must retain its parameter in filter help.');
check($variadicFilter->getAttribute('ModifierDoubleInsert') === '', 'A variadic-only filter must not produce insertion colons.');

$function = setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'directFunction');
check($function->hasAttribute('FunctionDescription') && $function->getAttribute('FunctionDescription') === '', 'Functions must explicitly write empty FunctionDescription metadata.');
check(countSettings($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'overriddenFunction') === 1, 'Later function registrations must replace earlier registrations.');
$overriddenFunction = setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'overriddenFunction');
check($overriddenFunction->getAttribute('FunctionReturnType') === 'int' && $overriddenFunction->getAttribute('FunctionHelp') === '(int $last)', 'Function metadata must come from the later registration.');
$templateFunction = setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'templateFunction');
check($templateFunction->getAttribute('FunctionHelp') === '(string $argument)', 'Injected Template parameters must be omitted from function help.');
check($templateFunction->getAttribute('FunctionReturnType') === 'int', 'Function return types must be exported.');
check(setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'contextSelf')->getAttribute('FunctionReturnType') === '\\ContextualFunctionFixture', 'self return types must resolve to their fully qualified class name.');
check(setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'contextNullableSelf')->getAttribute('FunctionReturnType') === '?\\ContextualFunctionFixture', 'Nullable self return types must resolve to their fully qualified class name.');
check(setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'contextParent')->getAttribute('FunctionReturnType') === '\\ContextualFunctionParent', 'parent return types must resolve to their fully qualified parent class name.');
check(setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'contextStatic')->getAttribute('FunctionReturnType') === '\\ContextualFunctionChild', 'Inherited static return types must resolve to the called subclass.');
check(setting($document, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'contextSelfOrString')->getAttribute('FunctionReturnType') === '\\ContextualFunctionFixture|string', 'Union self return types must resolve to their fully qualified class name.');

$emptyEngineDocument = document(LatteXmlExporter::generate(new Engine()));
check(nodes($emptyEngineDocument, '/project/component[@name="LattePluginSettings"]/customMacroSettings/list/LatteTagSettings')->length === 0, 'An unmodified engine must omit every built-in tag.');
check(nodes($emptyEngineDocument, '/project/component[@name="LattePluginSettings"]/customModifierSettings/list/LatteFilterSettings')->length === 0, 'An unmodified engine must omit every built-in filter.');
check(nodes($emptyEngineDocument, '/project/component[@name="LattePluginSettings"]/customFunctionSettings/list/LatteFunctionSettings')->length === 0, 'An unmodified engine must omit every built-in function.');

if (class_exists('Nette\\Bridges\\CacheLatte\\CacheExtension') && class_exists('Nette\\Caching\\Storages\\MemoryStorage')) {
    $cacheEngine = new Engine();
    $cacheEngine->addExtension(new \Nette\Bridges\CacheLatte\CacheExtension(new \Nette\Caching\Storages\MemoryStorage()));
    $cacheDocument = document(LatteXmlExporter::generate($cacheEngine));
    check(countSettings($cacheDocument, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'cache') === 0, 'Nette CacheExtension must not export the built-in cache tag.');

    $cacheEngine->addExtension(new class extends Extension
    {
        public function getTags(): array
        {
            return [
                'cache' => function () {
                },
            ];
        }
    });
    $cacheOverrideDocument = document(LatteXmlExporter::generate($cacheEngine));
    check(setting($cacheOverrideDocument, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'cache')->getAttribute('MacroType') === 'UNPAIRED', 'A custom cache tag must override Nette CacheExtension and be exported.');
}

$existingXml = <<<'XML'
<project version="4">
  <component name="OtherComponent"><option name="keep" value="yes" /></component>
  <component name="LattePluginSettings">
    <option name="enableNette" value="false" />
    <option name="variableSettings"><list><LatteVariableSettings VarName="manualVariable" /></list></option>
    <customMacroSettings><list>
      <LatteTagSettings MacroName="manualTag" MacroType="PAIR" />
      <LatteTagSettings MacroName="paired" MacroType="UNPAIRED" Arguments="stale" DeprecatedMessage="manual deprecation" ManualAttribute="keep"><ManualChild value="keep" /></LatteTagSettings>
    </list></customMacroSettings>
    <customModifierSettings><list>
      <LatteFilterSettings ModifierName="manualFilter" />
      <LatteFilterSettings ModifierName="directFilter" ModifierDescription="manual filter description" />
    </list></customModifierSettings>
    <customFunctionSettings><list>
      <LatteFunctionSettings FunctionName="manualFunction" />
      <LatteFunctionSettings FunctionName="directFunction" FunctionDescription="manual function description" />
    </list></customFunctionSettings>
  </component>
</project>
XML;

$merged = document(LatteXmlExporter::generate($engine, $existingXml));
check(one($merged, '/project/component[@name="OtherComponent"]/option[@name="keep"]')->getAttribute('value') === 'yes', 'Unrelated components must survive merging.');
check(one($merged, '/project/component[@name="LattePluginSettings"]/option[@name="enableNette"]')->getAttribute('value') === 'false', 'Existing Latte options must survive merging.');
one($merged, '/project/component[@name="LattePluginSettings"]/option[@name="variableSettings"]/list/LatteVariableSettings[@VarName="manualVariable"]');
one($merged, '/project/component[@name="LattePluginSettings"]/customMacroSettings/list/LatteTagSettings[@MacroName="manualTag"]');
one($merged, '/project/component[@name="LattePluginSettings"]/customModifierSettings/list/LatteFilterSettings[@ModifierName="manualFilter"]');
one($merged, '/project/component[@name="LattePluginSettings"]/customFunctionSettings/list/LatteFunctionSettings[@FunctionName="manualFunction"]');
check(countSettings($merged, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'paired') === 1, 'A generated tag must replace its matching existing entry.');
$mergedPaired = setting($merged, 'customMacroSettings', 'LatteTagSettings', 'MacroName', 'paired');
check($mergedPaired->getAttribute('MacroType') === 'PAIR', 'Merged generated tags must contain the newly exported definition.');
check($mergedPaired->getAttribute('Arguments') === 'stale', 'Merging must retain manual Arguments metadata on matching tags.');
check($mergedPaired->getAttribute('DeprecatedMessage') === 'manual deprecation', 'Merging must retain manual DeprecatedMessage metadata on matching tags.');
check($mergedPaired->getAttribute('ManualAttribute') === 'keep', 'Merging must retain extra attributes on matching entries.');
one($merged, '/project/component[@name="LattePluginSettings"]/customMacroSettings/list/LatteTagSettings[@MacroName="paired"]/ManualChild[@value="keep"]');
check(setting($merged, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'directFilter')->getAttribute('ModifierDescription') === 'manual filter description', 'Merging must retain manual ModifierDescription metadata on matching filters.');
check(setting($merged, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', 'directFunction')->getAttribute('FunctionDescription') === 'manual function description', 'Merging must retain manual FunctionDescription metadata on matching functions.');

$invalidXmlRejected = false;
try {
    LatteXmlExporter::generate($engine, '<project><component></project>');
} catch (Throwable $exception) {
    $invalidXmlRejected = true;
}
check($invalidXmlRejected, 'Invalid existing XML must be rejected.');

$integrationFixture = dirname(__DIR__, 3) . '/src/test/resources/data/settings/exported-latte.xml';
check(is_file($integrationFixture), 'Missing Java settings integration fixture: ' . $integrationFixture);
$fixtureEngine = new Engine();
$fixtureEngine->addExtension(new ExportFixtureComparisonExtension());
check(
    document(LatteXmlExporter::generate($fixtureEngine))->C14N() === document((string) file_get_contents($integrationFixture))->C14N(),
    'Exporter output must match the IntelliJ deserialization fixture.'
);

$bootstrap = tempnam(sys_get_temp_dir(), 'latte-export-bootstrap-');
$output = tempnam(sys_get_temp_dir(), 'latte-export-output-');
check($bootstrap !== false && $output !== false, 'Unable to create temporary files for CLI test.');
unlink($output);
file_put_contents($bootstrap, "<?php\nreturn (new \\Latte\\Engine())->addFilter('cliFilter', function (\$value) { return \$value; });\n");

$script = dirname(__DIR__, 3) . '/latte-xml-export.php';
$command = escapeshellarg(PHP_BINARY) . ' ' . escapeshellarg($script)
    . ' --autoload ' . escapeshellarg($argv[1])
    . ' --bootstrap ' . escapeshellarg($bootstrap);
exec($command, $cliOutput, $cliStatus);
check($cliStatus === 0, 'Exporter CLI failed when writing XML to stdout.');
$cliDocument = document(implode("\n", $cliOutput));
setting($cliDocument, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'cliFilter');

$outputCommand = $command . ' --output ' . escapeshellarg($output);
exec($outputCommand, $fileOutput, $fileStatus);
check($fileStatus === 0, 'Exporter CLI failed when writing XML to an output file.');
check($fileOutput === [], 'Exporter CLI must not write XML to stdout when --output is used.');
setting(document((string) file_get_contents($output)), 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', 'cliFilter');

file_put_contents($output, '<project><component></project>');
$invalidOutput = (string) file_get_contents($output);
exec($outputCommand, $invalidFileOutput, $invalidFileStatus);
check($invalidFileStatus !== 0, 'Exporter CLI must reject invalid existing XML.');
check((string) file_get_contents($output) === $invalidOutput, 'Exporter CLI must preserve an invalid existing output file after failure.');

$helpCommand = escapeshellarg(PHP_BINARY) . ' ' . escapeshellarg($script) . ' --help';
exec($helpCommand, $helpOutput, $helpStatus);
check($helpStatus === 0 && strpos(implode("\n", $helpOutput), '--autoload') !== false && strpos(implode("\n", $helpOutput), '--bootstrap') !== false, 'Exporter CLI help must document --autoload and --bootstrap.');

unlink($bootstrap);
unlink($output);

fwrite(STDOUT, "latte XML exporter tests passed\n");
