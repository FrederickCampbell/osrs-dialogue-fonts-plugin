# Dialogue Fonts+ v4 — Compatibility First

## Priority 1: Quest Helper / other plugin compatibility

v4 does NOT rewrite option text anymore.

Quest Helper's current dialogue-choice implementation:
- searches the live widget text for the desired answer
- prefixes the chosen answer with `[1]`, `[2]`, etc.
- sets a custom highlight color
- installs a mouse-leave listener to restore that highlight

v4 preserves all of those values exactly.

The plugin subscribes late to `BeforeRender`, snapshots the final text/color
state, temporarily sets only the TEXT widget opacity to 255 for native raster
suppression, paints the replacement, then immediately restores opacity.

It does NOT change:
- widget text
- text color
- listeners
- hidden state
- click mask
- Quest Helper numeric prefixes
- native option hotkeys

## Status channel

The bottom status widget is now fully supported and styleable:
- Click here to continue
- Click to continue
- Press space ... continue
- Please wait...
- future small listener-backed status strings

The same live listener/text widget stays intact; only its pixels are temporarily
suppressed and redrawn.

## Styling without config bloat

Font & Weight
- RuneLite native font picker
- body style
- speaker/title style
- options style
- continue/wait style
- System Default / Grayscale / Crisp rendering

Colors
- Plugin colors win (default ON)
- Body
- Speaker/title
- Options
- Option hover
- Continue/wait status

Every color uses:
TRANSPARENT = INHERIT LIVE GAME/PLUGIN COLOR

That removes the need for an extra enable toggle beside every color.

Effects
- independent shadow strength + shadow color
- independent outline strength + outline color
- both may be active simultaneously
- line spacing

## Overflow

Body text uses the exact selected font size. It is never silently shrunk.

Normal body:
- wrapped
- measured
- vertically centered like the native widget

Overflow body:
- top-aligned
- clipped to the real dialogue body viewport
- long unbroken words split safely
- mouse-wheel scrolling
- draggable/clickable native-looking mini scrollbar
- Page Up / Page Down

Optional "Continue pages overflow first":
- Click the native Continue line -> scroll one visual page while more text exists
- Space -> scroll one visual page while more text exists
- after reaching the bottom, the event passes through to Jagex normally
- no fake dialogue states are created
- number keys / Quest Helper option hotkeys are untouched

## Diagnostics

`%USERPROFILE%\.runelite\dialogue-fonts-plus\dialogue-widget-log.txt`

Now records widget opacity in snapshots and logs:
- SUPPRESS_GLYPHS
- RESTORE_OPACITY

This makes it easy to verify that the semantic widget state is preserved.


## v4.1 polish

- Fixed Java RGBA scrollbar constructor compile error.
- Added ~220ms option-selection acknowledgement for mouse and 1-9 hotkeys.
- Selection acknowledgement never consumes the original event and never mutates
  Quest Helper/native option state.
- The flash survives the immediate transition to `Please wait...`, specifically
  so server delay never looks like a missed click.
- Normal diagnostic logging now writes changed widget snapshots only.
- Full per-frame mutation logging is still available behind
  `Verbose per-frame mutations`, OFF by default.


## v4.2 settings-panel cleanup

No config keys changed, so existing user settings migrate automatically.

Panel layout is now:

1. Appearance
   - Font
   - Text smoothing
   - Preserve plugin colors
   - Dialogue / speaker / choice / hover / continue-wait colors

2. Dialogue Elements
   - NPC
   - Player
   - Choices
   - Item/action
   - Continue/wait

3. Interaction
   - Choice confirmation flash

4. Advanced Typography (collapsed)
   - Dialogue style
   - Speaker/title style
   - Choice style
   - Continue/wait style
   - Line spacing

5. Effects (collapsed)
   - Shadow size/color
   - Outline size/color

6. Large Text & Scrolling (collapsed)
   - Overflow scrollbar
   - Continue/Space visual paging
   - Mouse-wheel speed

7. Diagnostics (collapsed)
   - changed-state logging
   - verbose renderer logging

Labels and descriptions were rewritten to read like normal user-facing RuneLite
settings instead of internal implementation terminology.


## v4.3 native render-layer fix

The Trader Crewmember screenshot exposed two renderer bugs:

1. `Widget.opacity = 255` does not reliably suppress native bitmap TEXT glyphs.
   The replacement font could therefore be drawn over the original OSRS font.

2. `<br>` from Jagex was being flattened to spaces. That discarded the game's
   intentional line layout and made the TrueType renderer reflow text differently.

v4.3:
- snapshots the final semantic widget state
- temporarily sets only the widget render text to `""`
- native rendering therefore has zero glyphs to draw
- paints the TrueType replacement
- immediately restores the exact original string
- never changes text color/listeners/hotkeys
- preserves Quest Helper `[1]` etc. in the captured/rendered string
- preserves `<br>` as a mandatory line break
- still wraps inside each forced line if the selected font needs more room
- still invokes the existing overflow scrollbar if the exact font size cannot fit
- ignores transient white hover states when deciding whether to write a new
  diagnostic snapshot, reducing normal log churn further


## v4.4 adaptive render / Quest Helper compatibility

- Native text is no longer suppressed through opacity OR temporary text edits.
- High-priority BeforeRender restores normal widget font IDs.
- Quest Helper/other plugins run against the untouched widget.
- Low-priority BeforeRender captures final text/color/listeners/bounds.
- Only the native bitmap font ID is then set to -1 for rasterization.
- Dialogue Fonts+ draws that exact final state with your selected TrueType font.

Quest Helper therefore keeps:
- `[1]`, `[2]`, etc.
- its hotkeys/listeners
- its blue/special highlight color
while Dialogue Fonts+ owns:
- font family
- font size
- regular/bold/italic style
- outline/shadow/effects

Externally-styled options also keep their plugin highlight on hover when
`Preserve plugin colors` is enabled.

### Adaptive source wrapping
Jagex `<br>` breaks are preserved only while every original source line still
fits the selected font. If one source line would wrap, the old breakpoints are
treated as soft spaces and the whole paragraph reflows naturally. This removes
short orphan lines such as `you can`.

### Option wait transition
If an option row becomes `Please wait...`, all stale option rows have their
native font suppressed but only the wait line is custom-rendered. The selection
confirmation flash may remain briefly visible without stale text overlap.

### Overflow polish
Wheel/Page/Continue/track navigation snaps to complete text lines. Dragging the
scroll thumb remains smooth and snaps to a whole line on release.


## v4.4.1 startup fix

v4.4 compiled but RuneLite disabled it immediately at runtime.

Cause:
RuneLite's EventBus requires every annotated subscriber method to be named
exactly `on` + the subscribed event's class name. Therefore a second annotated
BeforeRender callback named `onBeforeRenderRestoreFonts` is illegal even though
Java/Gradle can compile it.

Fix:
- keep the normal low-priority annotated `onBeforeRender(BeforeRender)`
- register the +1000 priority restore callback programmatically through EventBus
- explicitly unregister that callback during plugin shutdown

No renderer/settings behavior from v4.4 was removed.
