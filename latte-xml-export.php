#!/usr/bin/env php
<?php declare(strict_types=1);

/**
 * Export a project's Latte 3 definitions for the Latte Support IDE plugin.
 * Based on Jakub Vrána's config generator: https://github.com/noctud/intellij-latte/pull/12
 * Distributed under this repository's MIT license.
 */
final class LatteXmlExporter
{
    // These extensions already have richer definitions in the IDE plugin.
    private const BUILTIN_EXTENSIONS = [
        'Latte\\Essential\\CoreExtension',
        'Latte\\Essential\\RawPhpExtension',
        'Latte\\Essential\\TranslatorExtension',
        'Latte\\Sandbox\\SandboxExtension',
        'Nette\\Bridges\\ApplicationLatte\\UIExtension',
        'Nette\\Bridges\\CacheLatte\\CacheExtension',
        'Nette\\Bridges\\FormsLatte\\FormsExtension',
    ];

    public static function generate(\Latte\Engine $engine, ?string $existingXml = null): string
    {
        $tags = $builtinFilters = $builtinFunctions = [];
        foreach ($engine->getExtensions() as $extension) {
            $builtin = in_array(get_class($extension), self::BUILTIN_EXTENSIONS, true);
            foreach ($extension->getTags() as $name => $parser) {
                $tags[$name] = [$parser, $builtin];
            }
            if ($builtin) {
                $builtinFilters = array_replace($builtinFilters, $extension->getFilters());
                $builtinFunctions = array_replace($builtinFunctions, $extension->getFunctions());
            }
        }

        $document = self::document($existingXml);
        $project = $document->documentElement;
        $component = self::child($project, 'component', 'name', 'LattePluginSettings');
        $macros = [];
        foreach ($tags as $name => [$parser, $builtin]) {
            $attribute = str_starts_with($name, 'n:');
            $name = $attribute ? substr($name, 2) : $name;
            $macros[$name] ??= ['normal' => false, 'attribute' => false, 'pair' => false, 'custom' => false];
            $macros[$name]['custom'] = $macros[$name]['custom'] || !$builtin;
            $macros[$name][$attribute ? 'attribute' : 'normal'] = true;
            if (!$attribute) {
                $parser = $parser instanceof \stdClass ? $parser->subject : $parser;
                $macros[$name]['pair'] = self::reflect($parser)->isGenerator();
            }
        }
        ksort($macros);
        foreach ($macros as $name => $tag) {
            if (!$tag['custom']) {
                continue;
            }
            $type = !$tag['normal'] ? 'ATTR_ONLY' : ($tag['pair'] ? 'PAIR' : ($tag['attribute'] ? 'UNPAIRED_ATTR' : 'UNPAIRED'));
            self::entry($component, 'customMacroSettings', 'LatteTagSettings', 'MacroName', [
                'MacroName' => $name, 'MacroType' => $type,
            ], ['Arguments' => '', 'DeprecatedMessage' => '']);
        }

        $filters = $engine->getFilters();
        ksort($filters);
        foreach ($filters as $name => $callback) {
            if (isset($builtinFilters[$name]) && self::sameCallable($callback, $builtinFilters[$name])) {
                continue;
            }
            $parameters = self::reflect($callback)->getParameters();
            if (isset($parameters[0]) && (string) $parameters[0]->getType() === 'Latte\\Runtime\\FilterInfo') {
                array_shift($parameters);
            }
            // Latte supplies the filtered value; only the remaining arguments are entered by the user.
            if (isset($parameters[0]) && !$parameters[0]->isVariadic()) {
                array_shift($parameters);
            }
            $required = count(array_filter($parameters, static fn($p) => !$p->isOptional() && !$p->isVariadic()));
            self::entry($component, 'customModifierSettings', 'LatteFilterSettings', 'ModifierName', [
                'ModifierName' => $name, 'ModifierHelp' => self::parameters($parameters),
                'ModifierDoubleInsert' => str_repeat(':', $required),
            ], ['ModifierDescription' => '']);
        }

        $functions = $engine->getFunctions();
        ksort($functions);
        foreach ($functions as $name => $callback) {
            if (isset($builtinFunctions[$name]) && self::sameCallable($callback, $builtinFunctions[$name])) {
                continue;
            }
            $reflection = self::reflect($callback);
            $parameters = $reflection->getParameters();
            if (isset($parameters[0]) && (string) $parameters[0]->getType() === 'Latte\\Runtime\\Template') {
                array_shift($parameters);
            }
            self::entry($component, 'customFunctionSettings', 'LatteFunctionSettings', 'FunctionName', [
                'FunctionName' => $name, 'FunctionReturnType' => self::returnType($reflection),
                'FunctionHelp' => '(' . self::parameters($parameters) . ')',
            ], ['FunctionDescription' => '']);
        }
        return $document->saveXML();
    }

    private static function reflect(callable $callback): \ReflectionFunction
    {
        return new \ReflectionFunction(\Closure::fromCallable($callback));
    }

    private static function returnType(\ReflectionFunction $reflection): string
    {
        $scope = $reflection->getClosureScopeClass();
        $context = [
            'self' => $scope?->getName(),
            'parent' => $scope && $scope->getParentClass() ? $scope->getParentClass()->getName() : null,
            // getClosureCalledClass() was added in PHP 8.0.23 / 8.1.11.
            'static' => method_exists($reflection, 'getClosureCalledClass')
                ? $reflection->getClosureCalledClass()?->getName()
                : ($reflection->getClosureThis() ? get_class($reflection->getClosureThis()) : $scope?->getName()),
        ];
        // The IDE resolves types outside the callback's class scope, including nullable/union types.
        return preg_replace_callback(
            '~(?<![a-zA-Z0-9_\\\\])(self|parent|static)(?![a-zA-Z0-9_\\\\])~',
            static fn($match) => isset($context[$match[1]]) ? '\\' . $context[$match[1]] : 'mixed',
            (string) $reflection->getReturnType() ?: 'mixed'
        );
    }

    private static function sameCallable(callable $left, callable $right): bool
    {
        $left = self::reflect($left);
        $right = self::reflect($right);
        // Extensions may return fresh closures on each call. Compare their implementation,
        // rather than object identity, to retain the IDE's built-in metadata.
        return $left->getName() === $right->getName()
            && $left->getFileName() === $right->getFileName()
            && $left->getStartLine() === $right->getStartLine()
            && $left->getEndLine() === $right->getEndLine()
            && $left->getClosureScopeClass()?->getName() === $right->getClosureScopeClass()?->getName();
    }

    private static function parameters(array $parameters): string
    {
        return implode(', ', array_map(static function (\ReflectionParameter $parameter): string {
            $text = ($parameter->hasType() ? $parameter->getType() . ' ' : '')
                . ($parameter->isPassedByReference() ? '&' : '')
                . ($parameter->isVariadic() ? '...' : '') . '$' . $parameter->getName();
            return $parameter->isOptional() && !$parameter->isVariadic() ? '[' . $text . ']' : $text;
        }, $parameters));
    }

    private static function document(?string $xml): \DOMDocument
    {
        $document = new \DOMDocument('1.0', 'UTF-8');
        $document->preserveWhiteSpace = false;
        $document->formatOutput = true;
        if ($xml === null) {
            $document->appendChild($document->createElement('project'))->setAttribute('version', '4');
        } else {
            $previous = libxml_use_internal_errors(true);
            try {
                if ($xml === '' || !$document->loadXML($xml, LIBXML_NONET) || $document->doctype !== null
                    || $document->documentElement->tagName !== 'project') {
                    throw new \RuntimeException('Existing settings must be a project XML document without a DOCTYPE.');
                }
            } finally {
                libxml_clear_errors();
                libxml_use_internal_errors($previous);
            }
        }
        return $document;
    }

    private static function child(\DOMElement $parent, string $tag, ?string $attribute = null, ?string $value = null): \DOMElement
    {
        $matches = [];
        foreach ($parent->childNodes as $child) {
            if ($child instanceof \DOMElement && $child->tagName === $tag
                && ($attribute === null || $child->getAttribute($attribute) === $value)) {
                $matches[] = $child;
            }
        }
        if (count($matches) > 1) {
            throw new \RuntimeException("Ambiguous existing settings: duplicate $tag elements.");
        }
        if ($matches) {
            return $matches[0];
        }
        $child = $parent->appendChild($parent->ownerDocument->createElement($tag));
        if ($attribute !== null) {
            $child->setAttribute($attribute, $value);
        }
        return $child;
    }

    private static function entry(\DOMElement $component, string $section, string $tag, string $key, array $attributes, array $defaults): void
    {
        $list = self::child(self::child($component, $section), 'list');
        $entry = self::child($list, $tag, $key, $attributes[$key]);
        foreach ($attributes as $name => $value) {
            $entry->setAttribute($name, $value);
        }
        foreach ($defaults as $name => $value) {
            if (!$entry->hasAttribute($name)) {
                $entry->setAttribute($name, $value);
            }
        }
    }
}

function latteXmlExportMain(array $arguments): int
{
    try {
        $options = [];
        for ($i = 1; $i < count($arguments); $i++) {
            $option = $arguments[$i];
            if ($option === '--help' || $option === '-h') {
                echo "Usage: php latte-xml-export.php [--autoload PATH] [--bootstrap PATH] [--output PATH]\n\n"
                    . "Export a configured Latte 3 engine. Requires Latte 3.0.4+, PHP 8.0+ and ext-dom.\n"
                    . "--autoload   Project vendor/autoload.php (default: current directory, then script directory/parent).\n"
                    . "--bootstrap  PHP file returning a Latte\\Engine; otherwise use App\\Bootstrap and Nette's LatteFactory.\n"
                    . "--output     Merge into this XML file; default: print a new XML document to stdout.\n"
                    . "Close the project in the IDE before updating .idea/latte.xml.\n";
                return 0;
            }
            if (!in_array($option, ['--autoload', '--bootstrap', '--output'], true)
                || !isset($arguments[$i + 1]) || str_starts_with($arguments[$i + 1], '--') || isset($options[$option])) {
                throw new \InvalidArgumentException("Invalid or repeated option: $option. Use --help.");
            }
            $options[$option] = $arguments[++$i];
        }
        if (!class_exists(\DOMDocument::class)) {
            throw new \RuntimeException('Enable PHP ext-dom to export XML.');
        }
        $autoload = $options['--autoload'] ?? null;
        if ($autoload === null) {
            foreach ([getcwd(), __DIR__, dirname(__DIR__)] as $directory) {
                if (is_file($directory . '/vendor/autoload.php')) {
                    $autoload = $directory . '/vendor/autoload.php';
                    break;
                }
            }
        }
        if ($autoload === null || !is_file($autoload)) {
            throw new \RuntimeException('Composer autoloader not found. Run from your project or use --autoload PATH.');
        }

        // Application bootstraps sometimes print diagnostics. Keep them out of the XML stream.
        ob_start();
        try {
            require_once $autoload;
            if (!class_exists(\Latte\Engine::class) || \Latte\Engine::VERSION_ID < 30004 || \Latte\Engine::VERSION_ID >= 40000) {
                throw new \RuntimeException('The project must have latte/latte >=3.0.4 and <4 installed.');
            }
            if (isset($options['--bootstrap'])) {
                if (!is_file($options['--bootstrap'])) {
                    throw new \RuntimeException('Bootstrap file not found: ' . $options['--bootstrap']);
                }
                $engine = require $options['--bootstrap'];
            } else {
                $bootstrap = dirname(realpath($autoload), 2) . '/app/Bootstrap.php';
                if (!class_exists('App\\Bootstrap') && is_file($bootstrap)) {
                    require_once $bootstrap;
                }
                if (!is_callable(['App\\Bootstrap', 'boot'])) {
                    throw new \RuntimeException('App\\Bootstrap::boot() not found. Use --bootstrap with a PHP file returning your configured Latte\\Engine.');
                }
                $container = \App\Bootstrap::boot()->createContainer();
                $engine = $container->getByType('Nette\\Bridges\\ApplicationLatte\\LatteFactory')->create();
            }
            if (!$engine instanceof \Latte\Engine) {
                throw new \RuntimeException('The bootstrap must return a configured Latte\\Engine.');
            }
        } finally {
            $diagnostics = ob_get_clean();
            if ($diagnostics !== '') {
                fwrite(STDERR, $diagnostics);
            }
        }

        $output = $options['--output'] ?? null;
        if ($output !== null && (is_link($output) || (file_exists($output) && !is_file($output)))) {
            throw new \RuntimeException('Output must be a regular file, not a symlink or directory.');
        }
        $existing = $output !== null && is_file($output) ? file_get_contents($output) : null;
        if ($existing === false) {
            throw new \RuntimeException('Could not read existing settings.');
        }
        $xml = LatteXmlExporter::generate($engine, $existing);
        if ($output === null) {
            echo $xml;
        } else {
            if (!is_dir(dirname($output)) || !is_writable(dirname($output))) {
                throw new \RuntimeException('Output directory must exist and be writable: ' . dirname($output));
            }
            $temporary = tempnam(dirname($output), '.latte-export-');
            if ($temporary === false) {
                throw new \RuntimeException('Could not create temporary output file.');
            }
            try {
                if (file_put_contents($temporary, $xml) !== strlen($xml)) {
                    throw new \RuntimeException('Could not write XML.');
                }
                if ($existing !== null && !chmod($temporary, fileperms($output) & 0777)) {
                    throw new \RuntimeException('Could not preserve settings file permissions.');
                }
                clearstatcache(true, $output);
                if (is_link($output) || (file_exists($output) ? file_get_contents($output) : null) !== $existing) {
                    throw new \RuntimeException('Settings changed during export. Close the IDE project and try again.');
                }
                if (!rename($temporary, $output)) {
                    throw new \RuntimeException('Could not replace settings file.');
                }
            } finally {
                if (is_file($temporary)) {
                    unlink($temporary);
                }
            }
            fwrite(STDERR, "Updated $output\n");
        }
        return 0;
    } catch (\Throwable $error) {
        fwrite(STDERR, 'Latte export failed: ' . $error->getMessage() . "\n");
        return 1;
    }
}

if (realpath($_SERVER['SCRIPT_FILENAME'] ?? '') === __FILE__) {
    exit(latteXmlExportMain($argv));
}
