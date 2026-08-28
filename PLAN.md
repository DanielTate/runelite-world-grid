# World Grid Plugin — Build Plan

Working name **"World Grid"**. Check hub collisions before submitting (O9).
Decisions referenced as D1-D26 from `DECISIONS.md`.

| | |
|---|---|
| Package | `nz.tate.worldgrid` |
| Config group | `world-grid` |
| Author | `dan` |
| Storage | `.runelite/world-grid/locations.json` |
| Toolchain | JDK 17 compiling to Java 11 (`options.release.set(11)`), Gradle 8.10 wrapper, JUnit 4.12 |

---

## Architecture — pure core, thin RuneLite shell

Same rule as the timezone plugin: **all grid maths, ref formatting/parsing, and the whole
storage layer live in classes that import nothing from `net.runelite`.** Only rendering,
menu entries and Swing touch the client.

This matters more here than in the timezone plugin, because D18 ships everything at once
and D15/D20 make the file format permanent. The parts that are permanent are exactly the
parts that are pure — so they can be pinned by tests before any of it is drawn on screen.

```
core/   (no RuneLite imports — 100% unit tested)
  GridAnchor        the permanent origin constant                       D1  O1
  GridCell          value type: column, row, plane
  GridMath          world x/y <-> cell, using Math.floorDiv             D1 D2
  GridRefFormatter  cell -> "L35", "GA12", "L35+2"                      D3 D4
  GridRefParser     "L35" -> cell, with typed errors                    D19 O21
  SavedLocation     label + x + y + plane + pinned                      D15 D17 D20
  LocationFile      schema envelope with version field                  D14 D18
  LocationCodec     Gson read/write, untrusted-input validation         D14 D20
  ImportPlan        merge/replace, collision counts                     D16 D23

plugin/ (RuneLite-facing — manual verification)
  WorldGridPlugin        lifecycle, event wiring
  WorldGridConfig        three independent toggles                      D5 D26
  SceneGridOverlay       ABOVE_SCENE grid + ground label                D5 D6 D9
  WorldMapGridOverlay    MANUAL + drawAfterLayer                        D5 D8
  GridRefHudOverlay      DETACHED movable readout                       D6 D26
  WorldGridPanel         saved-location library, multi-select           D13 D24
  LocationRepository     background-thread disk IO                      D14
  PinManager             WorldMapPoint lifecycle, marker subclass       D17 D21
  GridSearchMenuEntry    right-click search + transient pin             D19 D21
```

## Rules this plan is built around

| Rule | Where it lands |
|---|---|
| No crowdsourcing player data | The plugin never reads, stores or transmits anything about other players. It computes *your* ref from *your* coordinates. Nothing leaves the machine except through a file the user explicitly exports. |
| No menu entries that hit the server | Search uses `MenuAction.RUNELITE`, which is client-side only — the same mechanism core RuneLite's own "Focus on" uses. |
| File IO only under `.runelite` | Working file is `RuneLite.RUNELITE_DIR/world-grid/`. Import/export use `JFileChooser`, which is the sanctioned user-initiated route. |
| No disk IO on the client thread | All reads/writes go to a background thread; re-entry via `clientThread.invoke()`. |
| No Java serialization | Gson only, injected — never constructed. |
| Overlay runs every frame | Never scan the scene in `render()`. Cell computation is O(1) arithmetic; the visible cell range is derived from the camera/map viewport, not by iterating the world. |
| Config keys are permanent | Group `world-grid` and all key names fixed at first commit. |

### The bug we must not inherit

`world-location`'s world-map overlay uses `~(gridSize - 1)` masking, which is
**power-of-two only** and silently draws a wrong grid at sizes like 22. D2 allows any size
1-256. We use `Math.floorDiv` throughout — plain `/` truncates toward zero and breaks for
coordinates west or south of the anchor. **A test asserts correct cells at a
non-power-of-two size and at negative offsets from the anchor.**

## Commit sequence

| # | Commit | Tests |
|---|---|---|
| 1 | `chore: scaffold plugin from example-plugin template` | build compiles |
| 2 | `feat: grid anchor constant and cell arithmetic` (D1, D2) | floorDiv correctness at size 22, 63, 64, 256; negative offsets; sample refs pinned |
| 3 | `feat: grid reference formatting` (D3, D4) | column lettering past Z (`AA`, `AB`); plane suffix `+1`/`+2`; no negative planes |
| 4 | `feat: grid reference parsing with typed errors` (D19, O21) | case-insensitivity, whitespace, plane suffix, out-of-range, outside-world |
| 5 | `feat: round-trip property test across the world` (D15) | format-then-parse returns the same cell for every cell, at several sizes |
| 6 | `feat: saved location model and JSON schema` (D15, D18, D20) | schema version present; `pinned` written on export |
| 7 | `feat: location file codec with untrusted-input validation` (D14) | malformed JSON, missing fields, out-of-range coords all fail without throwing or wiping |
| 8 | `feat: import merge and replace planning` (D16, D23) | merge is additive; existing label wins; identity is `(x,y,plane)`; counts correct |
| 9 | `feat: pinned flag imports as unpinned` (D20) | export writes it, import always clears it |
| 10 | `feat: config interface with three independent toggles` (D5, D26) | — |
| 11 | `feat: plugin lifecycle and overlay registration` | — |
| 12 | `feat: scene grid overlay` (D5, D9) | — |
| 13 | `feat: compass orientation labels` (D7, D9) | label-to-direction mapping is pure and tested |
| 14 | `feat: HUD grid reference readout` (D6, D26) | — |
| 15 | `feat: instance detection suppresses the grid` (D10) | — |
| 16 | `feat: world map grid overlay` (D5, D8) | — |
| 17 | `feat: location repository with background disk IO` (D14) | — |
| 18 | `feat: saved location side panel` (D13) | — |
| 19 | `feat: per-location pin toggle` (D17) | — |
| 20 | `feat: row multi-select and bulk delete` (D24) | — |
| 21 | `feat: grid reference search from the world map` (D19) | — |
| 22 | `feat: save a searched ref as a location` (D21) | — |
| 23 | `feat: import and export via JFileChooser` (D22, D25) | export scope filtering; 200-entry warning threshold |
| 24 | `docs: README` | — |

Commits 2-9 are pure logic, fully covered, and lock the permanent format before a single
pixel is drawn. That ordering is deliberate: D18 makes the schema unchangeable at v1, so
it gets pinned by tests first.

## Test strategy

- **JUnit 4.12**, no client, no EDT, no real filesystem (codec tests use strings).
- **The round-trip property test is the centrepiece** (commit 5). D15 requires format and
  parse to be exact inverses; a sampled sweep across the world at sizes 22, 63, 64 and 256
  is what makes the permanent ref format trustworthy.
- **Anchor samples are pinned** so changing `GridAnchor` fails loudly rather than silently
  renaming the world.
- Import tests feed deliberately hostile JSON — truncated, wrong types, absent fields,
  coordinates outside 0-16383 — and assert the existing list survives intact.

## Performance notes

- Cell computation is two `floorDiv` calls. Cheap enough to do per frame.
- Scene overlay iterates only the visible cell range, derived from the scene bounds — never
  the world, never the scene tile array.
- World map overlay bails out early when `gridSize * pixelsPerTile < 3` (lines would be
  unreadable and the loop long).
- `Perspective.localToCanvas` returns null off-screen; every point is null-checked.
- Pins are added/removed on toggle, never rebuilt per frame.
- No executors beyond a single-thread IO worker, shut down with `shutdownNow()`.

## Open question feeding this build

**O1 (the anchor) is being derived now** and lands in commit 2. It is the only permanent
value still uncertain. Everything downstream of it is already settled.

## Known risks

Tracked live in `RISKS.md`.
