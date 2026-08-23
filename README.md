# Dialogue Fonts

Customizable TrueType rendering for native Old School RuneScape dialogue.

## Features

- RuneLite font picker with installed system fonts and `~/.runelite/fonts`
- configurable NPC/player dialogue, speaker, option, and status typography
- Quest Helper-compatible option rendering: prefixes, semantic colors, hotkeys,
  and listeners remain owned by Quest Helper while Dialogue Fonts owns typography
- `Click here to continue`, space-to-continue, and `Please wait...` rendering
- configurable colors, styles, smoothing, outline, shadow, and line spacing
- adaptive wrapping for custom font sizes
- overflow scrolling with an OSRS-like scrollbar
- click and number-key option confirmation feedback
- low-noise diagnostics for unusual dialogue widget states

## Compatibility

Dialogue Fonts is deliberately a last-mile renderer. The live widget text,
colors, listeners, and choice semantics remain available to Jagex and other
RuneLite plugins. The native bitmap font is suppressed only for rasterization,
then the captured final state is rendered with the selected TrueType font.

## Credits

Original plugin: **theOranguzang**

Extended renderer and compatibility work: **Frederick Campbell**

## License

BSD 2-Clause. See `LICENSE`.
