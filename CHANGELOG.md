# Changelogs

## Version 1.9.0 [BREAKING]

- feat: **Added support for user-installable applications**
  - To use this feature, set the context of a command by using `CommandBuilder#context(InteractionContextType...)` or `Command#setContext(InteractionContextType...)`
- feat: Deprecated `Limited#isGuildOnly()` in favor of changing the command context
- fix: Specifying an argument as an `OptionType#NUMBER` will parse the value as a `Double` instead of a `Long`
- fix: `CommandBuilder` now works without specifying all fields
