# Changelog

## [1.7.0] - 2026-03-31

### Added

- Support for `{syntax off}...{/syntax}` — disables Latte macro parsing inside the block
- Support for `{syntax double}...{/syntax}` — switches macro delimiters to `{{...}}`
- Support for `n:syntax="off"` and `n:syntax="double"` attributes on HTML elements

### Fixed

- NPE in file path resolution for virtual directories
- IllegalStateException when creating a new Latte file

## [1.6.5] - 2026-03-26

### Fixed

- Exception when viewing the settings form in the IDE

## [1.6.4] - 2026-03-25

### Fixed

- Variable assignment in conditions not detected as definition
- False "multiple definitions" and "probably undefined" warnings for variables defined in all branches of `{if}/{else}`

### Changed

- Removed unused context caching dead code from `LatteFile`

## [1.6.3] - 2026-03-23

### Fixed

- False "probably undefined" variable warning in nested scopes
- False warnings for function and arrow function parameters

## [1.6.2] - 2026-03-22

### Fixed

- Variable resolution now respects scope contexts (foreach, if, block)
- Variables defined in inner scopes are marked as "probably undefined" outside
- Inner variable definitions correctly shadow outer ones
- Type detection for typed variable definitions (e.g. `{define input, float $name}`)
- Lexer handling of PHP closures inside latte tags (`function() { }`)
- Type compatibility check for union types at different depths
- Deeply nested blocks/snippets producing errors on closing tags
- Null pointer errors during variable rename refactoring

### Changed

- CI now runs tests on every push and PR
- Added tests for inspections, parser edge cases, lexer, and utilities

## [1.6.1] - 2025-12-18

### Fixed

- Detection of absolute links
- Warning about LatteCodeStyleSettingsProvider
- Few other deprecations

## [1.6.0] - 2025-08-31

### Added

- References to presenter components via `{control ...}` etc. (bidirectional)
- Autocompletion of presenter components (and their render methods)
- Link references from presenter methods and class `actionSomething` => `{link something}`
- Usage info in unused PHP fields that are used in latte

### Fixed

- Autocompletion of global functions at the start of the macro
- Indentation of HTML content inside tags on new lines

### Improved

- Presenter name resolving, when using `{templateType}`

## [1.5.5] - 2025-08-25

### Fixed

- Autocompletion in `{var}`, `{varType}` and `{templateType}`

## [1.5.4] - 2025-08-23

### Fixed

- IntelliJ freezes while typing in non-closed latte tags

## [1.5.3] - 2025-08-21

### Fixed

- Autocompletion speed
- Autocompletion was not showing when `{$` was typed

## [1.5.2] - 2025-08-10

### Fixed

- Inconsistencies in adding custom filters and functions (@vrana)

## [1.5.1] - 2025-08-01

### Fixed

- Error inspection in multiline file includes (@vrana)

## [1.5.0] - 2025-07-30

### Added

- Enum support

## [1.4.1] - 2025-07-05

### Added

- Support for {asset} and n:asset

## [1.4.0] - 2025-04-07

### Added

- Support for typehints in iterables using generics

## [1.3.2] - 2025-01-22

### Fixed

- Disabled file existence checks in functions inside tags

## [1.3.1] - 2024-12-26

### Added

- File existence checks in tags like `{import}` or `{include}`

## [1.3.0] - 2024-05-22

### Added

- Link autocompletion in `{link}` and `n:href`

## [1.2.1] - 2024-05-05

### Fixed

- Reloading variables when {templateType} is changed
- Cache bugs in link references (e.g. when renaming files / methods) - disabled cache

## [1.2.0] - 2024-05-05

### Added

- Support for nette links - linking presenter, signal, action, etc.

## [1.1.0] - 2024-05-05

### Added

- File and directory linking in {import}, {include} and similar tags
- Auto-completion of directories and latte files in file import tags

## [1.0.6] - 2024-04-28

### Fixed

- Pair tag hover length (caused by previous fix)
- Iterable type detection (warnings only, can't read generics yet)
- End tag auto-completion (double slashes)

## [1.0.5] - 2024-04-27

### Fixed

- IndexOutOfBoundsException when not closing a tag right away
- RangeOverlapException in closed tag references

## [1.0.4] - 2024-04-26

### Changed

- Default link color (blue has better visibility)

## [1.0.3] - 2024-04-25

### Fixed

- Default variable color
- Default link color

## [1.0.2] - 2024-04-22

### Fixed

- Error `Cannot distinguish StubFileElementTypes` (performance issue)

## [1.0.1] - 2024-04-21

### Added

- Null-safe operator support
- Plugin .jars to latest release

## [1.0.0] - 2024-04-21

### Added

- Support for PhpStorm up to 2024.1
- Previously deleted features (code completion etc.)
- Automatic builds on push via GitHub actions

### Fixed

- Build process

### Changed

- Plugin name to Latte Support (fork of https://github.com/nette-intellij/intellij-latte)
- Gradle to version 8.7
- Grammar kit and intellij platform versions to latest

### Removed

- Unused libs, docs, ads, sponsoring info, some readme content
