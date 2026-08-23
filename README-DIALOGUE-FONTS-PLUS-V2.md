# Dialogue Fonts+ v2 polish / diagnostics build

This patch upgrades the existing local Dialogue Fonts+ fork.

## Main changes

### Native RuneLite font picker
The old free-text `Font family / file` field is gone.

Dialogue Fonts+ now uses RuneLite's native `FontType` configuration control.
RuneLite itself supplies:
- built-in RuneLite fonts
- custom fonts from `~/.runelite/fonts`
- installed system fonts
- size
- Bold toggle
- Italic toggle

This also means Italic is explicitly controllable and defaults OFF.

### Anti-aliasing fix
The old plugin used LCD HRGB subpixel text anti-aliasing. That can look fringed,
doubled, or "broken" when the game canvas is GPU-scaled.

This build defaults to grayscale AA:
`RenderingHints.VALUE_TEXT_ANTIALIAS_ON`

Fractional metrics are kept OFF for more stable pixel alignment.

### Continue prompt fix
By default Dialogue Fonts+ NO LONGER replaces the tiny native
"Click here / Press space / Please wait" prompt.

That prompt is timing-sensitive and uses a different widget/layout than body text.
Leaving it native is the safest and cleanest result.

If desired, enable:
Dialogue Fonts+ -> Dialogue Types -> Continue Prompt

When enabled, the plugin now:
- discovers the continue widget by live text instead of only a hard-coded child index
- supports Click here to continue / Click to continue / Press space / spacebar / Please wait
- preserves text content so keyboard behavior remains intact
- uses a smaller font for the compact prompt

### Deep widget diagnostic log
Enabled by default for this dev build.

Log:
%USERPROFILE%\.runelite\dialogue-fonts-plus\dialogue-widget-log.txt

It records:
- PRE-mutation widget snapshots
- widget IDs and parent IDs
- static/dynamic child indices
- text
- font IDs
- text colors
- bounds
- line height
- alignment
- listeners
- every BLANK_TEXT / CAMOUFLAGE_TEXT / RESTORE_TEXT_COLOR mutation

It logs only when the dialogue widget tree changes, rather than spamming every tick.

That gives us the exact data needed to diagnose any remaining odd dialogue type.
