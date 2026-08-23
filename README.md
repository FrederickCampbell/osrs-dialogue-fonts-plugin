# Better Dialogue Boxes

A RuneLite plugin that replaces native OSRS dialogue glyphs with configurable
TrueType rendering while preserving the live game and plugin semantics.

## Features

- RuneLite native font picker with installed system fonts and `~/.runelite/fonts`
- per-element font style, color, outline, shadow, and smoothing controls
- NPC, player, item/action, option, continue, press-space, and wait text
- Quest Helper-aware option rendering that preserves prefixes, colors, listeners,
  and hotkeys while using Better Dialogue Boxes typography
- adaptive dialogue wrapping for custom font sizes
- safe overflow scrolling with an OSRS-like scrollbar
- click / number-key choice confirmation feedback
- de-spammed diagnostic widget logging for unusual dialogue states

## Compatibility model

Better Dialogue Boxes owns **typography and rendering**.

Jagex, Quest Helper, and other plugins keep ownership of the underlying text,
choice prefixes, semantic colors, listeners, and hotkeys. The plugin snapshots
their final state immediately before native rendering and redraws it using the
selected TrueType font.

The internal Java package and existing RuneLite config group intentionally keep
their historical names so upgrades do not reset existing settings.

## Credits

Originally based on
[`theOranguzang/osrs-dialogue-fonts-plugin`](https://github.com/theOranguzang/osrs-dialogue-fonts-plugin).

Original Dialogue Fonts work: **theOranguzang**

Better Dialogue Boxes fork and extended renderer: **Frederick Campbell**

## License

BSD 2-Clause. See `LICENSE`.
