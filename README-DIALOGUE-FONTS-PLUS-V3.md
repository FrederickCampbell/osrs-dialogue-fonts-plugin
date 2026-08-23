# Dialogue Fonts+ v3 — Modern Render / Ghost Fix

This build changes the rendering architecture rather than continuing to patch
the old camouflage approach.

## What the diagnostic log proved

Native NPC/player body widgets are centered vertically in a 380x67 text region.
The old fork drew replacement text from the top of that region.

Option widgets stayed alive underneath our overlay and were only recolored to
the parchment background. Jagex changes those native colors on hover/state
changes, so the underlying glyphs could briefly/faintly reappear.

## v3 architecture

### BeforeRender suppression
Suppression now happens from RuneLite's `BeforeRender` event, after client
scripts/tick processing but BEFORE the frame is drawn.

The overlay itself no longer mutates widgets. This closes the race where a
widget could be repopulated after ClientTick, get painted by the game, and only
then be blanked by our ABOVE_WIDGETS overlay.

### NPC / player / item dialogue
Name/body text widgets are hidden with `Widget.setHidden(true)`.
Their text is never destroyed. Geometry/text/colors are snapshotted first.

This follows the important architectural lesson from Modern Chat: suppress the
legacy render layer instead of trying to color-match its glyphs away.

### Option menus
Clickable option widgets must remain alive for mouse/listener behavior.

Their visible text is replaced just before rendering with a Unicode NBSP
(non-breaking space). It is:
- non-empty for compatibility with native option-selection logic
- visually blank
- still attached to the original clickable widget/listener
- restored when the plugin shuts down

The custom overlay then paints exactly one visible copy of each option.

### Continue / wait prompts
Dialogue Fonts+ v3 NEVER modifies them.

"Click here to continue", "Press space to continue", and "Please wait..." stay
100% native. This removes the previous color-camouflage race and preserves
native spacebar/click behavior.

### Correct layout
Wrapped body text is measured first, then the entire block is vertically
centered in the native body bounds.

Names/options use the native widget midpoint.

The overlay also inherits native widget colors instead of hard-coding the old
incorrect blue name color.

### Modern Chat-style Java2D path
System Default rendering leaves Graphics2D's text hints alone, like Modern Chat.
Optional rendering modes:
- System Default (default)
- Grayscale
- Crisp / No AA

Optional fast shadow/outline uses repeated `drawString`, modeled after Modern
Chat's cached-glyph approach.

### Diagnostics
Log remains:
`%USERPROFILE%\.runelite\dialogue-fonts-plus\dialogue-widget-log.txt`

It now also records HIDDEN_STATE and NEUTRALIZE_INTERACTIVE_TEXT mutations.
