package dev.noctud.latte.config;

import dev.noctud.latte.settings.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noctud.latte.settings.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LatteDefaultConfiguration {

    private static @Nullable LatteDefaultConfiguration instance = null;

    private static final @NotNull Set<LatteConfiguration.Vendor> vendors = Stream
        .of(LatteConfiguration.Vendor.LATTE, LatteConfiguration.Vendor.NETTE_FORMS, LatteConfiguration.Vendor.NETTE_APPLICATION)
        .collect(Collectors.toSet());

    private final Map<LatteConfiguration.Vendor, Map<String, LatteTagSettings>> defaultTags = new HashMap<>();
    private final Map<LatteConfiguration.Vendor, Map<String, LatteFilterSettings>> defaultFilters = new HashMap<>();
    private final Map<LatteConfiguration.Vendor, Map<String, LatteVariableSettings>> defaultVariables = new HashMap<>();
    private final Map<LatteConfiguration.Vendor, Map<String, LatteFunctionSettings>> defaultFunctions = new HashMap<>();

    public LatteDefaultConfiguration() {
        loadDefaults();
    }

    public static LatteDefaultConfiguration getInstance() {
        if (instance == null) {
            instance = new LatteDefaultConfiguration();
        }
        return instance;
    }

    public LatteConfiguration.Vendor[] getVendors() {
        return vendors.toArray(new LatteConfiguration.Vendor[0]);
    }

    public Map<String, LatteTagSettings> getTags(LatteConfiguration.Vendor vendor) {
        return defaultTags.getOrDefault(vendor, Collections.emptyMap());
    }

    public Map<String, LatteFilterSettings> getFilters(LatteConfiguration.Vendor vendor) {
        return defaultFilters.getOrDefault(vendor, Collections.emptyMap());
    }

    public Map<String, LatteVariableSettings> getVariables(LatteConfiguration.Vendor vendor) {
        return defaultVariables.getOrDefault(vendor, Collections.emptyMap());
    }

    public Map<String, LatteFunctionSettings> getFunctions(LatteConfiguration.Vendor vendor) {
        return defaultFunctions.getOrDefault(vendor, Collections.emptyMap());
    }

    private void loadDefaults() {
        loadDefaultLatteTags();
        loadDefaultLatteFilters();
        loadDefaultLatteFunctions();

        loadDefaultNetteApplicationTags();
        loadDefaultNetteApplicationFunctions();
        loadDefaultNetteApplicationVariables();

        loadDefaultNetteFormsTags();
        loadDefaultNetteAssetsTags();
    }

    private void loadDefaultLatteTags() {
        addLatteTag(filtersTag("_", LatteTagSettings.Type.AUTO_EMPTY, requiredArgument("expression", "string", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(filtersTag("=", LatteTagSettings.Type.UNPAIRED, requiredArgument("expression", "string", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(filtersMultiTag("block", LatteTagSettings.Type.AUTO_EMPTY, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_EXPRESSION, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_IDENTIFIER)));
        addLatteTag(tag("breakIf", LatteTagSettings.Type.UNPAIRED, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(filtersMultiTag("capture", LatteTagSettings.Type.PAIR, requiredArgument("variable", LatteArgumentSettings.Type.VARIABLE_DEFINITION)));
        addLatteTag(tag("case", LatteTagSettings.Type.UNPAIRED, requiredRepeatableArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(tag("catch", LatteTagSettings.Type.UNPAIRED, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(tag("contentType", LatteTagSettings.Type.UNPAIRED, requiredArgument("content-type", "string", LatteArgumentSettings.Type.CONTENT_TYPE)));
        addLatteTag(tag("continueIf", LatteTagSettings.Type.UNPAIRED, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(tag("debugbreak", LatteTagSettings.Type.UNPAIRED, requiredArgument("expression", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("default", LatteTagSettings.Type.UNPAIRED, requiredRepeatableArgument("variable", LatteArgumentSettings.Type.VARIABLE_DEFINITION_EXPRESSION)));
        addLatteTag(tag("parameters", LatteTagSettings.Type.UNPAIRED, requiredRepeatableArgument("variable", LatteArgumentSettings.Type.VARIABLE_DEFINITION_EXPRESSION)));
        addLatteTag(multiTag("define", LatteTagSettings.Type.PAIR, requiredArgument("name", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("variable", LatteArgumentSettings.Type.VARIABLE_DEFINITION_ITEM)));
        addLatteTag(tag("do", LatteTagSettings.Type.UNPAIRED, requiredArgument("expression", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("dump", LatteTagSettings.Type.UNPAIRED, requiredArgument("expression", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("else", LatteTagSettings.Type.UNPAIRED_ATTR));
        addLatteTag(tag("elseif", LatteTagSettings.Type.UNPAIRED, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(tag("elseifset", LatteTagSettings.Type.UNPAIRED, requiredArgument("var", "string", LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.BLOCK)));
        addLatteTag(tag("extends", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION, LatteArgumentSettings.Type.NONE)));
        addLatteTag(tag("first", LatteTagSettings.Type.UNPAIRED, requiredArgument("width", "int", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(multiTag("for", LatteTagSettings.Type.PAIR, "initialization; condition; afterthought"));
        addLatteTag(filtersMultiTag("foreach", LatteTagSettings.Type.PAIR, "expression as [$key =>] $value"));
        addLatteTag(tag("if", LatteTagSettings.Type.PAIR, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(tag("ifset", LatteTagSettings.Type.PAIR, requiredArgument("var", "string", LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.BLOCK, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("import", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", "string", LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(filtersTag("include", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", "string", LatteArgumentSettings.Type.BLOCK, LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("arguments", LatteArgumentSettings.Type.KEY_VALUE)));
        addLatteTag(tag("includeblock", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", "string", LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("l", LatteTagSettings.Type.UNPAIRED));
        addLatteTag(tag("last", LatteTagSettings.Type.PAIR, requiredArgument("width", "int", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("layout", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.NONE)));
        addLatteTag(tag("class", LatteTagSettings.Type.ATTR_ONLY, "class"));
        addLatteTag(tag("attr", LatteTagSettings.Type.ATTR_ONLY, "attr"));
        addLatteTag(tag("ifcontent", LatteTagSettings.Type.ATTR_ONLY));
        addLatteTag(tag("php", LatteTagSettings.Type.UNPAIRED, requiredArgument("expression", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("r", LatteTagSettings.Type.UNPAIRED));
        addLatteTag(tag("sandbox", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", "string", LatteArgumentSettings.Type.PHP_EXPRESSION, LatteArgumentSettings.Type.BLOCK, LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE), repeatableArgument("key-value", LatteArgumentSettings.Type.KEY_VALUE)));
        addLatteTag(tag("sep", LatteTagSettings.Type.PAIR, argument("width", "int", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(multiTag("snippet", LatteTagSettings.Type.PAIR, argument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(multiTag("snippetArea", LatteTagSettings.Type.PAIR, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("spaceless", LatteTagSettings.Type.PAIR));
        addLatteTag(multiTag("switch", LatteTagSettings.Type.PAIR, argument("expression", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(multiTag("syntax", LatteTagSettings.Type.PAIR, "off | double | single"));
        addLatteTag(tag("templatePrint", LatteTagSettings.Type.UNPAIRED, argument("class-name", LatteArgumentSettings.Type.PHP_CLASS_NAME)));
        addLatteTag(tag("templateType", LatteTagSettings.Type.UNPAIRED, requiredArgument("class-name", LatteArgumentSettings.Type.PHP_CLASS_NAME)));
        addLatteTag(tag("try", LatteTagSettings.Type.PAIR));
        addLatteTag(tag("rollback", LatteTagSettings.Type.UNPAIRED));
        addLatteTag(tag("tag", LatteTagSettings.Type.ATTR_ONLY, requiredRepeatableArgument("expression", "string", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("ifchanged", LatteTagSettings.Type.PAIR, requiredRepeatableArgument("expression", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addLatteTag(tag("skipIf", LatteTagSettings.Type.UNPAIRED, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(tag("var", LatteTagSettings.Type.UNPAIRED, requiredRepeatableArgument("variable", LatteArgumentSettings.Type.VARIABLE_DEFINITION_EXPRESSION)));
        addLatteTag(tag("trace", LatteTagSettings.Type.UNPAIRED));
        addLatteTag(tag("varPrint", LatteTagSettings.Type.UNPAIRED, "all"));
        addLatteTag(tag("varType", LatteTagSettings.Type.UNPAIRED, requiredArgument("file", LatteArgumentSettings.Type.PHP_TYPE), requiredArgument("variable", LatteArgumentSettings.Type.VARIABLE_DEFINITION)));
        addLatteTag(multiTag("while", LatteTagSettings.Type.PAIR, requiredArgument("condition", "bool", LatteArgumentSettings.Type.PHP_CONDITION)));
        addLatteTag(multiTag("iterateWhile", LatteTagSettings.Type.PAIR));
        addLatteTag(multiTag("embed", LatteTagSettings.Type.PAIR, requiredArgument("file", "string", LatteArgumentSettings.Type.BLOCK_USAGE, LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("key-value", LatteArgumentSettings.Type.KEY_VALUE)));
    }

    private void loadDefaultLatteFilters() {
        addLatteFilter("truncate", ":($length, $append = '…')", "shortens the length preserving whole words", ":");
        addLatteFilter("substr", ":($offset [, $length])", "returns part of the string", ":");
        addLatteFilter("trim", ":($charset = mezery)", "strips whitespace or other characters from the beginning and end of the string");
        addLatteFilter("stripHtml", "", "removes HTML tags and converts HTML entities to text");
        addLatteFilter("strip", "", "removes whitespace");
        addLatteFilter("indent", ":($level = 1, $char = '\t')", "indents the text from left with number of tabs");
        addLatteFilter("replace", ":($search, $replace = '')", "replaces all occurrences of the search string with the replacement", ":");
        addLatteFilter("replaceRE", ":($pattern, $replace = '')", "replaces all occurrences according to regular expression", ":");
        addLatteFilter("padLeft", ":($length, $pad = ' ')", "completes the string to given length from left", ":");
        addLatteFilter("padRight", ":($length, $pad = ' ')", "completes the string to given length from right", ":");
        addLatteFilter("repeat", ":($count)", "repeats the string", ":");
        addLatteFilter("implode", ":($glue = '')", "joins an array to a string");
        addLatteFilter("webalize", "adjusts the UTF-8 string to the shape used in the URL");
        addLatteFilter("breaklines", "inserts HTML line breaks before all newlines");
        addLatteFilter("reverse", "reverse an UTF-8 string or array");
        addLatteFilter("length", "returns length of a string or array");
        addLatteFilter("sort", "simply sorts array");
        addLatteFilter("reverse", "array sorted in reverse order (used with |sort)");
        addLatteFilter("batch", ":($array, $length [, $item])", "returns length of a string or array", "::");

        addLatteFilter("date", ":(int $min, int $max)", "returns value clamped to the inclusive range of min and max.", "::");

        addLatteFilter("lower", "makes a string lower case");
        addLatteFilter("upper", "makes a string upper case");
        addLatteFilter("firstUpper", "makes the first letter upper case");
        addLatteFilter("capitalize", "lower case, the first letter of each word upper case");

        addLatteFilter("date", ":($format)", "formats date", ":");
        addLatteFilter("number", ":($decimals = 0, $decPoint = '.', $thousandsSep = ',')", "format number");
        addLatteFilter("bytes", ":($precision = 2)", "formats size in bytes");
        addLatteFilter("dataStream", ":($mimetype = 'detect')", "Data URI protocol conversion");

        addLatteFilter("noescape", "prints a variable without escaping");
        addLatteFilter("escapeurl", "escapes parameter in URL");

        addLatteFilter("nocheck", "prevents automatic URL sanitization");
        addLatteFilter("checkurl", "sanitizes string for use inside href attribute");

        addLatteFilter("query", "generates a query string in the URL");
        addLatteFilter("ceil", ":(int $precision = 0)", "rounds a number up to a given precision");
        addLatteFilter("explode", ":(string $separator = '')", "splits a string by the given delimiter");
        addLatteFilter("first", "returns first element of array or character of string");
        addLatteFilter("floor", ":(int $precision = 0)", "rounds a number down to a given precision");
        addLatteFilter("join", ":(string $glue = '')", "joins an array to a string");
        addLatteFilter("last", "returns last element of array or character of string");
        addLatteFilter("random", "returns random element of array or character of string");
        addLatteFilter("round", ":(int $precision = 0)", "rounds a number to a given precision");
        addLatteFilter("slice", ":(int $start, int $length = null, bool $preserveKeys = false)", "extracts a slice of an array or a string", ":");
        addLatteFilter("spaceless", "removes whitespace");
        addLatteFilter("split", ":(string $separator = '')", "splits a string by the given delimiter");
    }

    private void loadDefaultLatteFunctions() {
        addLatteFunction("clamp", "int|float", "(int|float $value, int|float $min, int|float $max)", "clamps value to the inclusive range of min and max");
        addLatteFunction("divisibleBy", "bool", "(int $value)", "checks if a variable is divisible by a number");
        addLatteFunction("even", "bool", "(int $value)", "checks if the given number is even");
        addLatteFunction("first", "mixed", "(string|array $value)", "returns first element of array or character of string");
        addLatteFunction("last", "mixed", "(string|array $value)", "returns last element of array or character of string");
        addLatteFunction("odd", "bool", "(int $value)", "checks if the given number is odd");
        addLatteFunction("slice", "string|array", "(string|array $value, int $start, int $length = null, bool $preserveKeys = false)", "extracts a slice of an array or a string");
    }

    private void loadDefaultNetteApplicationTags() {
        addNetteTag(tag("cache", LatteTagSettings.Type.PAIR, "if => expr, key, …", requiredArgument("name[:part]", "string", LatteArgumentSettings.Type.KEY_VALUE), repeatableArgument("arguments", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addNetteTag(tag("control", LatteTagSettings.Type.UNPAIRED, requiredArgument("name[:part]", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("arguments", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addNetteTag(tag("link", LatteTagSettings.Type.UNPAIRED, requiredArgument("destination", "string", LatteArgumentSettings.Type.LINK, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("arguments", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addNetteTag(tag("href", LatteTagSettings.Type.ATTR_ONLY, requiredArgument("destination", "string", LatteArgumentSettings.Type.LINK, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("arguments", LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addNetteTag(tag("nonce", LatteTagSettings.Type.ATTR_ONLY));
        addNetteTag(tag("plink", LatteTagSettings.Type.UNPAIRED, requiredArgument("destination", "string", LatteArgumentSettings.Type.LINK, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("arguments", LatteArgumentSettings.Type.PHP_EXPRESSION)));

        LatteTagSettings ifCurrent = tag("ifCurrent", LatteTagSettings.Type.PAIR, requiredArgument("destination", "string", LatteArgumentSettings.Type.LINK, LatteArgumentSettings.Type.PHP_EXPRESSION), repeatableArgument("arguments", LatteArgumentSettings.Type.PHP_EXPRESSION));
        ifCurrent.setDeprecated(true);
        ifCurrent.setDeprecatedMessage("Tag {ifCurrent} is deprecated in Latte 2.6. Use custom function isLinkCurrent() instead.");
        addNetteTag(ifCurrent);
    }

    private void loadDefaultNetteApplicationVariables() {
        addNetteVariable("control", "\\Nette\\Application\\UI\\Control");
        addNetteVariable("basePath", "string");
        addNetteVariable("baseUrl", "string");
        addNetteVariable("flashes", "mixed[]");
        addNetteVariable("presenter", "\\Nette\\Application\\UI\\Presenter");
        addNetteVariable("iterator", "\\Latte\\Runtime\\CachingIterator");
        addNetteVariable("form", "\\Nette\\Application\\UI\\Form");
        addNetteVariable("user", "\\Nette\\Security\\User");
    }

    private void loadDefaultNetteApplicationFunctions() {
        addNetteFunction("isLinkCurrent", "bool", "(string $destination = null, $args = [])");
        addNetteFunction("isModuleCurrent", "bool", "(string $moduleName)");
    }

    private void loadDefaultNetteFormsTags() {
        addFormsTag(multiTag("form", LatteTagSettings.Type.PAIR, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addFormsTag(multiTag("formContainer", LatteTagSettings.Type.PAIR, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addFormsTag(tag("formPrint", LatteTagSettings.Type.UNPAIRED, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addFormsTag(tag("input", LatteTagSettings.Type.UNPAIRED, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.CONTROL, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addFormsTag(tag("inputError", LatteTagSettings.Type.UNPAIRED, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.CONTROL, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addFormsTag(tag("label", LatteTagSettings.Type.AUTO_EMPTY, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.CONTROL, LatteArgumentSettings.Type.PHP_EXPRESSION)));
        addFormsTag(tag("name", LatteTagSettings.Type.ATTR_ONLY, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.CONTROL, LatteArgumentSettings.Type.PHP_EXPRESSION)));
    }

    private void loadDefaultNetteAssetsTags() {
        addNetteTag(tag("asset", LatteTagSettings.Type.UNPAIRED_ATTR, requiredArgument("name", "string", LatteArgumentSettings.Type.PHP_IDENTIFIER, LatteArgumentSettings.Type.VARIABLE, LatteArgumentSettings.Type.PHP_EXPRESSION)));
    }

    private void addLatteTag(LatteTagSettings tag) {
        addTag(LatteConfiguration.Vendor.LATTE, tag);
    }

    private void addLatteFilter(String name, String arguments, String description, String insertColons) {
        addFilter(LatteConfiguration.Vendor.LATTE, name, arguments, description, insertColons);
    }

    private void addLatteFilter(String name, String arguments, String description) {
        addFilter(LatteConfiguration.Vendor.LATTE, name, arguments, description, "");
    }

    private void addLatteFilter(String name, String description) {
        addFilter(LatteConfiguration.Vendor.LATTE, name, "", description, "");
    }

    private void addLatteFunction(String name, String returnType, String arguments, String description) {
        addFunction(LatteConfiguration.Vendor.LATTE, name, returnType, arguments, description);
    }

    private void addNetteTag(LatteTagSettings tag) {
        addTag(LatteConfiguration.Vendor.NETTE_APPLICATION, tag);
    }

    private void addNetteVariable(String name, String type) {
        addVariable(LatteConfiguration.Vendor.NETTE_APPLICATION, name, type);
    }

    private void addNetteFunction(String name, String returnType, String arguments) {
        addFunction(LatteConfiguration.Vendor.NETTE_APPLICATION, name, returnType, arguments, "");
    }

    private void addFormsTag(LatteTagSettings tag) {
        addTag(LatteConfiguration.Vendor.NETTE_FORMS, tag);
    }

    private void addTag(LatteConfiguration.Vendor vendor, LatteTagSettings tag) {
        Map<String, LatteTagSettings> map;
        if (defaultTags.containsKey(vendor)) {
            map = defaultTags.get(vendor);
        } else {
            map = new HashMap<>();
            defaultTags.put(vendor, map);
        }
        map.put(tag.getMacroName(), tag);
    }

    private void addVariable(LatteConfiguration.Vendor vendor, String name, String type) {
        Map<String, LatteVariableSettings> map;
        if (defaultVariables.containsKey(vendor)) {
            map = defaultVariables.get(vendor);
        } else {
            map = new HashMap<>();
            defaultVariables.put(vendor, map);
        }
        LatteVariableSettings variable = new LatteVariableSettings(name, type);
        map.put(variable.getVarName(), variable);
    }

    private void addFilter(LatteConfiguration.Vendor vendor, String name, String arguments, String description, String insertColons) {
        Map<String, LatteFilterSettings> map;
        if (defaultFilters.containsKey(vendor)) {
            map = defaultFilters.get(vendor);
        } else {
            map = new HashMap<>();
            defaultFilters.put(vendor, map);
        }
        LatteFilterSettings filter = new LatteFilterSettings(name, description, arguments, insertColons);
        map.put(filter.getModifierName(), filter);
    }

    private void addFunction(LatteConfiguration.Vendor vendor, String name, String returnType, String arguments, String description) {
        Map<String, LatteFunctionSettings> map;
        if (defaultFunctions.containsKey(vendor)) {
            map = defaultFunctions.get(vendor);
        } else {
            map = new HashMap<>();
            defaultFunctions.put(vendor, map);
        }
        LatteFunctionSettings function = new LatteFunctionSettings(name, returnType, arguments, description);
        map.put(function.getFunctionName(), function);
    }

    private LatteTagSettings filtersTag(String macroName, LatteTagSettings.Type macroType, LatteArgumentSettings... argumentSettings) {
        return new LatteTagSettings(macroName, macroType, true, false, LatteConfiguration.Vendor.LATTE, Arrays.asList(argumentSettings));
    }

    private LatteTagSettings tag(String macroName, LatteTagSettings.Type macroType, LatteArgumentSettings... argumentSettings) {
        return new LatteTagSettings(macroName, macroType, false, false, LatteConfiguration.Vendor.LATTE, Arrays.asList(argumentSettings));
    }

    private LatteTagSettings tag(String macroName, LatteTagSettings.Type macroType, String arguments) {
        return new LatteTagSettings(macroName, macroType, false, false, LatteConfiguration.Vendor.LATTE, arguments);
    }

    private LatteTagSettings tag(String macroName, LatteTagSettings.Type macroType, String arguments, LatteArgumentSettings... argumentSettings) {
        return new LatteTagSettings(macroName, macroType, false, false, LatteConfiguration.Vendor.LATTE, arguments, Arrays.asList(argumentSettings));
    }

    private LatteTagSettings filtersMultiTag(String macroName, LatteTagSettings.Type macroType, LatteArgumentSettings... argumentSettings) {
        return new LatteTagSettings(macroName, macroType, true, true, LatteConfiguration.Vendor.LATTE, Arrays.asList(argumentSettings));
    }

    private LatteTagSettings filtersMultiTag(String macroName, LatteTagSettings.Type macroType, String arguments) {
        return new LatteTagSettings(macroName, macroType, true, true, LatteConfiguration.Vendor.LATTE, arguments);
    }

    private LatteTagSettings multiTag(String macroName, LatteTagSettings.Type macroType, LatteArgumentSettings... argumentSettings) {
        return new LatteTagSettings(macroName, macroType, false, true, LatteConfiguration.Vendor.LATTE, Arrays.asList(argumentSettings));
    }

    private LatteTagSettings multiTag(String macroName, LatteTagSettings.Type macroType, String arguments) {
        return new LatteTagSettings(macroName, macroType, false, true, LatteConfiguration.Vendor.LATTE, arguments);
    }

    private LatteArgumentSettings argument(String name, String validType, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, validType, false, false);
    }

    private LatteArgumentSettings argument(String name, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, "", false, false);
    }

    private LatteArgumentSettings requiredArgument(String name, String validType, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, validType, true, false);
    }

    private LatteArgumentSettings requiredArgument(String name, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, "", true, false);
    }

    private LatteArgumentSettings repeatableArgument(String name, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, "", false, true);
    }

    private LatteArgumentSettings requiredRepeatableArgument(String name, String validType, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, validType, true, true);
    }

    private LatteArgumentSettings requiredRepeatableArgument(String name, LatteArgumentSettings.Type... types) {
        return new LatteArgumentSettings(name, types, "", true, true);
    }

}
