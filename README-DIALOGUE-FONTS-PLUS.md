# Dialogue Fonts+

Local fork of `theOranguzang/osrs-dialogue-fonts-plugin`.

## Added here

- Any installed Windows/Java system font family can be entered in the RuneLite setting.
- Any `.ttf` or `.otf` placed in `%USERPROFILE%\.runelite\fonts` is loaded automatically.
- Custom fonts can be selected by:
  - exact filename (`MyFont-Regular.ttf`)
  - filename without extension (`MyFont-Regular`)
  - embedded font family (`My Font`)
  - embedded full font name
- The custom font directory is rescanned automatically every ~3 seconds.
- Config group is separate from the Plugin Hub version.
- Plugin appears as **Dialogue Fonts+**.

## Use

1. Run `RUN-DIALOGUE-FONTS-PLUS-DEV.bat`.
2. In the dev RuneLite client, enable **Dialogue Fonts+**.
3. Open its settings.
4. Set **Font family / file** to a Windows font family such as `Segoe UI`, `Arial`, `Aptos`, etc.
5. Or put a TTF/OTF in `%USERPROFILE%\.runelite\fonts` and enter its filename/family.

The installer writes `AVAILABLE-SYSTEM-FONTS.txt` into the same font directory when Windows' System.Drawing font API is available.
