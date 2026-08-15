# Store assets

Generated, not hand-drawn — regenerate any time with:

```bash
/c/Android/jdk21/jdk-21.0.12+8/bin/java.exe tools/StoreAssets.java
```

| File | Size | Used for |
|---|---|---|
| `icon-512.png` | 512×512, 32-bit PNG | Play Console → Store listing → App icon |
| `icon-1024.png` | 1024×1024 | Devpost / press (`docs/07-SUBMISSION-KIT.md`) |
| `feature-graphic-1024x500.png` | 1024×500 | Play Console → Store listing → Feature graphic |
| `screenshots/` | phone captures | Play listing — produced by `tools/capture_screenshots.sh`, not committed |

The mark is the app's own launcher icon (`app/src/main/res/drawable/ic_launcher_foreground.xml`)
— a coin dropping into a cabinet slot — redrawn at store resolution from the same coordinates,
with the palette in `core/design/Color.kt` and the bundled Chakra Petch faces. Changing the
launcher icon means re-running this so the two don't drift apart.

**Everything here is original artwork.** No game box art, no third-party logo, and no
influencer name, likeness or branding appears in any of it — see
`docs/01-PLAY-STORE-CRITICAL-PATH.md` §Compliance traps, where both are disqualifying.

Which field takes which file, and every other listing answer, is in
`docs/13-STORE-LISTING.md`.
