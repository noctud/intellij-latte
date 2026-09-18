# Contributing

Thanks for helping improve the plugin! I maintain it in my spare time, so the most
helpful contribution is often a small template that shows something going wrong.
You don't need to write a fix to report a bug.

## Reporting a problem

Please [open an issue](https://github.com/noctud/intellij-latte/issues/new/choose) with:

- The smallest Latte template that reproduces the problem, as text or an attached file.
- What you expected and what the plugin does instead, including any steps needed to reproduce it.
- Your IDE, plugin and Latte versions.
- Any PHP declarations or custom Latte definitions needed to reproduce it.

Problems you encounter in actual projects are especially useful. Please remove
private code and data from examples. Screenshots can help, but include the template
as text too so I can try it locally.

## Before writing a fix

For parser or lexer changes, new features and larger refactors, please open an
issue first and wait until we've agreed on the scope before preparing a PR.
I may prefer to work through a fix myself so I understand the affected code and
can maintain it afterwards.

Small, isolated fixes and documentation improvements can come directly as PRs.
Please keep each PR focused on one problem and leave unrelated cleanup for another
time.

## Pull requests

Explain the problem, how the change fixes it and what you tested. For bug fixes,
include a regression test that fails without the fix where practical. Run
`./gradlew test` for plugin changes and mention any checks you couldn't run.
See the [README](README.md#building) for building and running the sandbox IDE.

Using AI tools is fine, but please review the result yourself and be prepared to
explain it. Passing tests help, but I also need to understand how a change affects
other templates before merging it.

Review time is limited, so I can't promise a review or merge date. I may close a
PR if its scope is more than I can take on, even when it fixes a real problem.
Agreeing on the approach first helps avoid spending time on work I can't accept.
