# World Grid Plugin — Design Decisions

Captured during scoping. One entry per resolved question. Nothing here is code yet.

---

## D1. Grid anchor

**Question:** Where is the grid's origin? This determines what every reference looks like.

**Decision: always anchored at (768, 1792), south-west of the playable surface with
margin. Not user-configurable. Only the *grid size* is adjustable.**

```
size 64, anchor (768, 1792)

  Lumbridge       ->  W39
  Varrock square  ->  Z39
  Grand Exchange  ->  AA38
  Falador         ->  Y35
  Edgeville       ->  AA37
  Kourend Castle  ->  AD14
  Wyrmscraig      ->  H29     (Sailing, south of the old anchor)
```

**Why this matters.** Anchoring at the raw world origin `(0,0)` instead would make
cells line up exactly with RuneScape regions (Lumbridge would be region 12850), but the
playable surface begins well north of the origin, so *every* real reference would carry
two or three letters. Anchoring near the SW corner keeps most of the populated surface
in single-letter rows at size 64: short refs, easy to say out loud over voice comms.

**Consequence — the anchor is a hard-coded constant, so it can never drift between
players.** This is the significant win. The only setting two players must agree on is
the grid size (see D2). A mismatched *anchor* would have been invisible — both sides
would see a plausible grid and simply disagree about names — and that failure mode is
now designed out entirely.

**Consequence — the constant is locked and carries deliberate margin.** This resolves O1.
The earlier working value `(1024, 2496)` was correct against a 2023 cache but is now a
**correctness bug**: Sailing (November 2025) pushed the surface south and west, and named
playable content such as Wyrmscraig `(2567, 2295)` falls below y=2496, producing a
negative row index that bijective base-26 cannot represent at all.

Map-square enumeration of the March 2026 cache and the wiki's published surface basemap
bounds independently put the current corner at `(960, 2048)`. The shipped anchor adds
three regions of margin west and four south, because the map has expanded south-west
three times in a decade — Zeah 2016, Varlamore 2024, Sailing 2025 — and this constant
cannot be corrected after release. `768` and `1792` are both divisible by **256**, so grid
lines coincide with region boundaries at every power-of-two grid size the config offers.

Treat it as a permanent API commitment. `LandmarkReferenceTest` pins it.

Reference points established from the RuneLite API:

```
Constants.REGION_SIZE     = 64
Constants.CHUNK_SIZE      = 8
Constants.SCENE_SIZE      = 104
Constants.OVERWORLD_MAX_Y = 4160     // STALE - see below
full coordinate space     = 16384 x 16384
```

**`OVERWORLD_MAX_Y` is no longer a usable bound.** Sailing's northern oceans reach
y=4223, above that constant, so testing against it now wrongly rejects the northernmost
band of surface. The plugin never uses it; the parser validates against the full 16384
space, which stays correct however the map grows.

**Consequence — arithmetic must use `Math.floorDiv`, not bit masking.** With a
non-zero anchor and arbitrary (non-power-of-two) grid sizes, the `& ~(size - 1)`
masking trick used by the reference `world-location` plugin's map overlay is wrong.
See D2 and the notes in `../RESEARCH.md`.

---

## D2. Grid size setting

**Question:** How is grid size offered, given both players must match it for refs to agree?

**Decision: a free numeric setting, range 1-256, default 64.**

```
Settings > World Grid
  Grid size  [ 64 ]  (1-256)

size 64 -> Lumbridge L35
size 32 -> Lumbridge W69
size 22 -> Lumbridge AG100
```

Any value is allowed, including non-power-of-two sizes such as 22.

**Accepted risk: near-miss values are a silent mismatch.** Two players on 63 and 64
both see a perfectly valid-looking grid and quietly disagree about every name. The
option of stamping the size onto every displayed reference (`L35 /64`) was considered
and **not** taken here — refs stay clean. Worth revisiting if this bites in practice;
logged as O2.

**Consequences**

- **All grid maths must use `Math.floorDiv` / modulo, never bit masking.** The
  reference `world-location` plugin computes `gridTruncate = ~(gridSize - 1)` and masks
  with `&` in its world-map overlay — that is power-of-two-only and silently produces a
  wrong grid at size 22. Its *scene* overlay uses `/ gridSize * gridSize`, which is
  safe for positive coordinates. With a non-zero anchor, offset first, then floor-divide:

  ```
  col = Math.floorDiv(worldX - ANCHOR_X, gridSize)
  row = Math.floorDiv(worldY - ANCHOR_Y, gridSize)
  ```

  `floorDiv` (not `/`) matters because coordinates west or south of the anchor go
  negative, and Java's `/` truncates toward zero rather than flooring.

- **Label length varies with size**, since the row letter uses bijective base-26
  (A..Z, AA, AB, ...). Measured across the overworld surface:

  ```
  size    cols   rows   last row label
    8      368    208    GZ
   16      184    104    CZ
   22      134     76    BX
   32       92     52    AZ
   64       46     26    Z      <- single letters, the default
  128       23     13    M
  ```

  Only 64 and 128 stay inside single letters. This is a further argument for 64 as the
  default and a reason to expect two- and three-letter rows at small sizes.

- **Very small sizes are a rendering cost.** At size 1-8 the world map would try to
  draw thousands of cells. The reference implementation bails out when
  `gridSize * pixelsPerTile < 3`; the same guard is needed here, and label drawing
  should be suppressed well before the lines are.

- The 1-256 range is enforced with `@Range` on the `@ConfigItem`, which RuneLite
  renders as a bounded spinner.

---

## D3. Plane / height handling

**Question:** Lumbridge basement, ground floor and castle roof share the same x,y.
How does the reference express height?

**Decision: append the plane only when not at ground level.**

```
Lumbridge ground   -> L35
Castle 1st floor   -> L35+1
Castle 2nd floor   -> L35+2
Basement           -> (see D4 — no negative plane exists)

"Meet me at L35"     (ground)
"I'm at L35+2"       (upstairs)
```

Short by default, precise when it matters.

**Consequences**

- The common case — two people meeting on the surface — keeps clean two-token refs,
  which is the whole reason the SW anchor was chosen in D1.
- Any ref *without* a suffix is an explicit claim of "ground level", so the suffix
  cannot simply be omitted when unknown.
- The parser (for any future "go to this ref" or chat-echo feature) must accept both
  the bare and suffixed forms, and `+0` should be tolerated on input while never being
  produced on output.

**Resolved in D4: the negative form is dropped.** The game has no plane -1 —
`Client.getPlane()` returns 0-3 and `Constants.MAX_Z` is 4. RuneScape basements are
separate map areas elsewhere in the coordinate space, so they land in their own grid
square at plane 0. The `L35-1` form shown in the sketch above is illustrative of the
suffix *syntax* only and is never emitted. See D4.

---

## D4. Underground and interior locations

**Question:** The game has no plane -1 — basements and dungeons are separate map areas
at plane 0, placed elsewhere in the coordinate space. How are they named?

**Decision: drop the negative form. Only `+1`, `+2`, `+3` are ever emitted.**

An underground area is simply its own grid square at ground level, named from its real
coordinates, wherever the game actually puts it.

```
Lumbridge ground     -> L35
Castle 1st floor     -> L35+1
Castle 2nd floor     -> L35+2
Lumbridge basement   -> D88      (its real location in the coordinate space)
```

**Rationale.** This is technically accurate and needs no special-casing, no lookup
table, and no hand-maintained data. `Client.getPlane()` returns 0-3 and the ref simply
reflects it. `Constants.MAX_Z` is 4, so `+3` is the ceiling.

**Accepted cost: "where is D88?" is a fair question.** Underground refs point at
coordinates that do not appear anywhere on the surface world map, so a ref shared from
a dungeon is not directly findable by someone standing outside. Two mitigations were
offered and declined — flagging underground refs with a marker such as `~D88`, and
suppressing refs underground altogether. Neither is ruled out forever; logged as O4 if
this proves annoying in use.

**Implementation note.** `Constants.OVERWORLD_MAX_Y` is 4160 — anything above that in y
is cave/instance space. This is not needed to *compute* the ref under this decision, but
it is the cheap test if an underground indicator is ever added later.

---

## D5. Render targets

**Question:** Where is the grid drawn?

**Decision: two independent config checkboxes — world map and game scene. Unchecking
both turns the grid off. No minimap rendering.**

```
Settings > World Grid
  [x] Draw on world map
  [x] Draw in game scene
  Grid size  [ 64 ]
```

There is no separate master enable. The two checkboxes *are* the on/off control, which
keeps the settings panel honest — there is no state where the plugin claims to be on
but draws nothing unexplained.

### World map

Where the labelled squares earn their keep: you can read `L35` straight off the map and
find where someone means.

```
[World Map]
 +-----+-----+-----+
 |L34  |L35  |L36  |
 |     | (o) |     |
 +-----+-----+-----+
 |M34  |M35  |M36  |
 +-----+-----+-----+
```

Technique confirmed from the `world-location` plugin:

- `Overlay` with `setLayer(OverlayLayer.MANUAL)` and
  `drawAfterLayer(ComponentID.WORLD_MAP_MAPVIEW)`, position `DYNAMIC`,
  priority `PRIORITY_HIGH`.
- `client.getWorldMap().getWorldMapZoom()` gives pixels-per-tile and
  `.getWorldMapPosition()` gives the centre tile; from those, map tile coords to screen.
- Clip to the map widget's bounds (`map.getBounds()`) or lines spill outside the map.
- Bail out when `gridSize * pixelsPerTile < 3` to avoid drawing thousands of cells when
  zoomed out. Labels should be suppressed at a higher threshold than the lines.
- Label drawing pattern (translucent box + text) is directly reusable from the
  reference plugin's region-ID labelling.

### Game scene

Lines on the ground in the 3D view, so you can see the boundary you are about to cross.

```
[3D game view]
     \    |    /
      \   |   /
  -----+--+--+-----
       |  @  |
  -----+-----+-----
      /   |   \
```

- `Overlay` at `OverlayLayer.ABOVE_SCENE`, position `DYNAMIC`.
- Only the loaded scene exists (`Constants.SCENE_SIZE` == 104), so this is local
  context, never overview. Cull to roughly +/-16 tiles around the player.
- `LocalPoint.fromWorld(client, x, y)` -> `Perspective.localToCanvas(...)`, accumulating
  points into a `GeneralPath` and drawing once per axis. Lines must be walked
  tile-by-tile rather than drawn corner-to-corner, because the ground is not flat.
- Offset each point by half a tile (`LOCAL_TILE_SIZE / 2`) so lines land on tile edges
  rather than tile centres.

### Minimap — declined

Considered and dropped. Always-on peripheral awareness, but too small to carry cell
labels, and the two chosen surfaces already cover both the overview and the local case.
The reference plugin has a working minimap implementation if this is ever revisited.

**Performance note.** RuneLite's `AGENTS.md` warns that overlay `render()` runs every
frame and must stay minimal. Neither overlay should scan the scene; both derive purely
from the player's world position and the grid maths.

---

## D6. Reading your own grid reference

**Question:** How do you find your current ref so you can tell someone where you are?

**Decision: two ways — a movable HUD overlay, and the ref labelled on the ground square
you are standing in.** The `!grid` chat command was offered and not taken.

### Movable HUD overlay

```
[game screen]

  +-------------+
  | Grid   L35  |
  +-------------+
```

Small draggable box, always on screen, updating as you cross boundaries. Same mechanism
as the timezone plugin's clock overlay: `OverlayPanel` with
`setPosition(OverlayPosition.DETACHED)`, which sets `movable = true` and `snappable = false`.
`OverlayManager` persists `preferredLocation` across sessions; the user alt-drags it.

Independent of the D5 render checkboxes — the HUD readout is useful even with both
drawing surfaces off. (Confirm during build whether it needs its own toggle.)

### Ground label

The grid cell containing the player is labelled in the scene, so the ref is readable off
the world itself without consulting other UI.

```
[3D game view]
  -----+--------+-----
       |  L35   |
       |    @   |
  -----+--------+-----
```

Only meaningful when the scene grid is being drawn, so it is gated on the "draw in game
scene" checkbox from D5.

### No `!grid` command

Declined. If revisited, it must use the same locally-rewritten-`MessageNode` pattern
as the timezone plugin's `!TZ` (see `../timezone-plugin/DECISIONS.md` D4) — the outgoing
message must never be modified.

---

## D7. Compass orientation labels

**Question:** What do the N/S/E/W labels on a grid square mean?

**Decision: they are an orientation aid — they tell you which way is north.**

The square's edges are labelled with true world directions, pinned to the world and
**not** to the camera. The OSRS camera rotates freely; these labels do not.

```
[3D view, camera rotated]

      ---- N ----
     |          |
   W |   AB24   | E
     |     @    |
      ---- S ----
```

This makes phrases like *"I'm in AB24 heading south east"* directly readable off the
ground: you see which edge is S and which is E and know your bearing without checking the
compass or mentally correcting for camera yaw.

**What this is not.** The labels do **not** subdivide the square into sub-regions.
`AB24 SE` is not a finer-grained location — the cell is the unit of precision, and a
direction in a sentence describes *travel*, not *position*. A sub-cell addressing scheme
was offered and declined; if a 64-tile square ever proves too coarse to meet up in, the
answer is to reduce the grid size (D2), not to add a second addressing layer.

**Consequences**

- Labels are placed at the **edge midpoints** of the cell, not its corners, and only
  four of them (N/S/E/W) — no diagonals, since diagonals only make sense for sub-regions.
- They must be drawn in world space and reprojected each frame like the grid lines
  themselves, so they stay on the correct edges as the camera turns. `Perspective` handles
  this; the text is drawn at the projected canvas point of each edge midpoint.
- In OSRS world coordinates, **+y is north and +x is east**, so N is the high-y edge.
  This also confirms the D1 row lettering runs south-to-north: row A is the southernmost.
- Interaction with the ground ref label from D6 needs care — the cell ref sits in the
  middle of the square and the four direction labels on its edges, so they should not
  collide, but this wants checking in-game at small grid sizes.
- Open: whether these are drawn on **every** visible cell or only the player's current
  cell. Drawing on every cell at size 22 would be extremely noisy. Recommend player's
  cell only; logged as O5.

---

## D8. Label density

**Question:** Are the ref and compass labels drawn on every visible square, or only the
one you are standing in?

**Decision: only the player's current square.** Grid lines are drawn everywhere; labels
are drawn on one cell.

```
[3D view]
  ----+--------+----+---
      |        |    |
  ----+---N----+----+---
      |  L35   |    |
    W |    @   | E  |
  ----+---S----+----+---
      |        |    |

Neighbours are drawn but unlabelled.
```

**Rationale.** Stays readable at any grid size — labelling every visible cell at size 22
would be a wall of overlapping text. It also makes the labels unambiguous: they always
mean *here*, so there is no question which cell an `N` belongs to.

**Accepted cost.** You cannot read a neighbour's name without walking into it, so
"head to M36" requires the world map (D5) rather than the scene view.

**Consequence.** Only one cell's worth of label geometry is projected per frame — five
text draws total (one ref, four edge labels) — which keeps the scene overlay well within
the per-frame budget `AGENTS.md` asks for.

---

## D9. Labels are camera-independent — the core requirement

Called out separately because it is the entire reason the compass labels exist, and it
is a hard constraint on how they are drawn.

**The OSRS camera rotates freely. The labels must not.** `N` always sits on the
northern edge of the square no matter which way the player has spun the view. This is
what makes *"AB24, heading south east"* a sentence both players can act on: you read the
bearing off the ground rather than mentally correcting for camera yaw, and two people
with differently-rotated cameras still agree on what south east means.

**Implementation consequence.** The labels cannot be drawn at fixed screen positions or
be laid out relative to the viewport. Each edge midpoint is a **world coordinate**, is
reprojected through `Perspective.localToCanvas` every frame exactly like the grid lines,
and the glyph is drawn at wherever that projects to. When the camera turns, the four
labels visibly swing around the square and stay glued to their true-world edges.

**Consequence for text rendering.** Only the label's *position* rotates — the glyphs
themselves stay upright and screen-aligned (no rotated `Graphics2D` transform). Upright
text is legible at any camera angle, and the position alone carries the direction
information.

**Consequence for edge cases.** As the camera pitches toward horizontal, the far edge
label crowds the near one and projected points can fall off-screen or behind the camera.
`Perspective.localToCanvas` returns `null` in those cases and each label must be skipped
independently rather than aborting the whole overlay.

**Reminder from D7:** in OSRS world coordinates **+y is north, +x is east**. The N label
goes on the high-y edge. This also fixes the D1 lettering direction — row A is the
southernmost row.

---

## D10. Instanced areas

**Question:** Raids, Theatre of Blood and player-owned houses report fake coordinates.
What does the grid do there?

**Decision: detect the instance, show `—`, and stop drawing the grid.**

```
[inside Chambers of Xeric]

  +--------------------+
  | Grid   — (instance)|
  +--------------------+
```

No lines drawn in the scene, no ref on the HUD. The plugin is honest about not knowing
rather than producing a number someone might read out.

**Rationale.** The failure this designs out is the silent one. A ref computed from
instance coordinates looks completely normal and is meaningless — the worst possible
outcome for a plugin whose entire purpose is two people agreeing on a location. Showing
nothing is strictly better than showing something wrong.

**Declined alternative.** RuneLite can resolve the underlying template tile via
`WorldPoint.fromLocalInstance(client, localPoint)`, which would yield a stable ref — but
it points at where the raid's *template* lives in the coordinate space, not at anywhere
either player can walk to. Useful for debugging, actively misleading for meeting up.

**Implementation.** Guard on `client.isInInstancedRegion()`. It gates the scene overlay,
the world-map overlay and the HUD readout alike, so all three stay consistent. The
`—` state needs to be visually distinct from "plugin is off" — the parenthetical
`(instance)` does that work.

**Note.** This is separate from D4's underground handling. Dungeons and basements are
*not* instances — they are real, stable coordinates elsewhere in the world and do get a
normal ref. Only genuinely instanced content is suppressed here.

---

## D11. Visual styling

**Question:** How much appearance configuration is exposed?

**Decision: colour and line thickness, shared across both render surfaces.**

```
Settings > World Grid
  [x] Draw on world map
  [x] Draw in game scene
  Grid size       [ 64 ]
  Line colour     [ █ cyan ]
  Line thickness  [ 2 ]
```

These two cover the cases that actually matter: making lines visible against varied
terrain, and keeping them thin enough not to obscure the ground. This matches what other
RuneLite grid plugins expose.

**Implementation notes**

- The colour item is annotated `@Alpha` so RuneLite's picker offers a transparency
  slider — semi-transparent lines are the norm for scene overlays and this is the
  standard way to get it.
- Thickness applies via `new BasicStroke(width)` on the scene overlay. The world-map
  overlay draws with `drawRect` at map scale, where thickness matters much less; a
  shared value is fine, and the map may reasonably clamp it.
- One colour serves both surfaces. Per-surface styling was offered and declined as too
  many knobs; if the shared colour turns out to read badly against the world map's
  lighter background, splitting it is a small, backward-compatible change (keep the
  existing config key as the scene colour and add a new one for the map, so nobody's
  saved setting resets).
- Label text colour is not exposed. Derive it from the line colour at full opacity, or
  use the translucent-box-plus-black-text treatment the reference plugin uses for its
  region labels.

**Config hygiene (from RuneLite's `AGENTS.md`)**

- The config group name must be specific — `"world-grid"`, not `"grid"`.
- Config keys must never be renamed without a migration; renaming silently resets every
  user's saved settings. This applies to the grid-size key in particular, since it is the
  value two players must match.

---

## D12. Grid-reference search with a map pin

**Status: scope accepted, feasibility verified. Interaction details open (see O10-O12).**

Type a reference such as `GA12` and the world map jumps to that square and drops a pin.

**This is possible, and core RuneLite already does both halves.** No hacks required.

### Dropping the pin

`WorldMapPointManager` (`@Inject`-able singleton) with `add()` / `remove()` /
`removeIf()`. A `WorldMapPoint` carries exactly the fields this feature needs:

```java
WorldMapPoint          // net.runelite.client.ui.overlay.worldmap
  BufferedImage image        // the pin icon
  WorldPoint    worldPoint   // where it sits
  WorldPoint    target       // where clicking it jumps to
  Point         imagePoint   // anchor within the image; centred if null
  boolean       snapToEdge   // pin sticks to the map edge when off-screen
  boolean       jumpOnClick  // click to focus
  String        name         // menu option text
  String        tooltip      // hover text
```

Two of these are unusually well-suited here:

- **`snapToEdge`** — if the searched square is outside the current view, the pin clings to
  the edge of the map pointing towards it rather than vanishing. Exactly the behaviour you
  want when someone reads out a ref on the far side of the world.
- **`tooltip`** — hover the pin to confirm the ref, so the pin is self-describing.

`onEdgeSnap()` / `onEdgeUnsnap()` are overridable if the icon should change while snapped.

### Moving the map

`client.getWorldMap().setWorldMapPositionTarget(WorldPoint)`.

Precedent is first-class: core RuneLite's own `WorldMapOverlay` builds a **"Focus on"**
right-click entry for every map point and calls exactly this:

```java
client.createMenuEntry(-1)
    .setTarget(ColorUtil.wrapWithColorTag(worldPoint.getName(), JagexColors.MENU_TARGET))
    .setOption("Focus on")
    .setType(MenuAction.RUNELITE)
    .onClick(m -> client.getWorldMap().setWorldMapPositionTarget(
        MoreObjects.firstNonNull(worldPoint.getTarget(), worldPoint.getWorldPoint())));
```

`MenuAction.RUNELITE` entries are client-side only and send nothing to the server, so this
does not run into the "no menu entries that cause actions to be sent to the server" rule.

### Where the search text is typed — undecided

Four options, all viable:

| Option | Mechanism | Notes |
|---|---|---|
| `::grid GA12` | `CommandExecuted` | Trivial, zero risk, no UI. Reuses D4's chat plumbing. |
| In-game prompt | `ChatboxPanelManager.openTextInput("Grid ref:").onDone(...)` | Native RuneLite look, `addCharValidator` can restrict to letters/digits. |
| Side panel box | Swing `JTextField` | Pairs naturally with D13. |
| Map right-click | `MenuAction.RUNELITE` entry on the map widget | Most discoverable; needs one of the above to actually type into. |

**Not chosen: hooking the game's own map search.** `InterfaceID.Worldmap.SEARCH` exists,
but repurposing Jagex's search field is far more fragile and rule-adjacent than adding our
own input. Avoid.

---

## D13. Saved locations in a side panel

**Status: scope accepted, feasibility verified. Details open (see O10-O13).**

A RuneLite side panel listing saved grid references, each with a user-supplied label and
an "open in map" button.

```
[grid icon in toolbar]

  World Grid
  +---------------------------+
  | search: GA12       [Go]   |
  +---------------------------+

  SAVED
  +---------------------------+
  | Wine of Zamorak spot      |
  | L35              [map][x] |
  +---------------------------+
  | Meet point - clan event   |
  | T4               [map][x] |
  +---------------------------+

  [+ Save current location]
```

**Feasibility.** Standard `ClientToolbar` + `NavigationButton` + `PluginPanel`, the same
construction as the timezone plugin's panel. "Open in map" calls the same
`setWorldMapPositionTarget` as D12 and adds a pin.

**This is a significant scope increase.** World Grid was previously config-only with no
custom UI. A panel with an editable, persistent, labelled list is comfortably the largest
single piece of work in the plugin — larger than the grid rendering itself. Worth deciding
whether it lands in v1 or a later release.

### Implementation notes

- **Threading.** Panel buttons fire on the Swing EDT; `setWorldMapPositionTarget` and
  anything touching `Client` must be marshalled with `clientThread.invoke()`. Getting this
  wrong produces intermittent, hard-to-reproduce breakage.
- **Persistence.** Save the list through `ConfigManager` as JSON using the injected `Gson`
  (never construct your own, per `AGENTS.md`). Store the **grid reference string and the
  label**, not derived pixel or cell values.
- **Store refs, not raw coordinates — or store both.** Grid size is user-configurable
  (D2), so a saved `L35` means a different place if the size changes. Storing the
  underlying `WorldPoint` alongside the ref makes saved locations survive a size change;
  storing only the ref does not. Flagged as O11 because it changes the data model.
- **Pin lifecycle.** Remove every `WorldMapPoint` this plugin added in `shutDown()` —
  `removeIf()` with a marker subclass is the clean way. Leaked pins persist until the
  client restarts.

---

## D14. Saved locations are a local JSON file with import/export

**Decision: saved locations live in a JSON file on disk, and the side panel offers
import and export.**

This supersedes D13's note about persisting through `ConfigManager`.

```
.runelite/world-grid/locations.json
```

```
[grid icon in toolbar]

  World Grid
  +---------------------------+
  | search: GA12       [Go]   |
  +---------------------------+

  SAVED
  | Wine of Zamorak spot      |
  | L35              [map][x] |
  | Meet point - clan event   |
  | T4               [map][x] |

  [+ Save current location]
  [ Import ]      [ Export ]
```

**Why this is the right call.** Saved locations are exactly the thing you want to hand to
a clanmate — the whole plugin exists so two people can agree on a place. A shareable file
turns a personal bookmark list into a distributable one: a clan can publish an agreed set
of named locations and everyone imports the same file.

**Rules position — this is explicitly permitted.** RuneLite's `AGENTS.md`:

> - Only read/write files inside the `.runelite` directory. Create a subdirectory for your
>   plugin (e.g. `.runelite/your-plugin-name/`) if you need to store data on disk.
> - Use `RuneLite.RUNELITE_DIR` to get the path.
> - Alternatively, use `JFileChooser` for user-initiated file operations.

So: the working file sits under `RuneLite.RUNELITE_DIR`, and Import/Export use
`JFileChooser` so the user picks any path they like. Both halves are sanctioned.

**Consequences**

- **Sharing raises the stakes on the schema.** D13's open question (store the grid ref,
  the world point, or both) is no longer just about surviving your own grid-size change —
  an imported file may have been written by someone on a *different* grid size. See O11,
  which is now the most consequential open question in the plugin.
- Use the injected `Gson`, never a hand-rolled one, and never Java serialisation
  (forbidden outright).
- File IO must not run on the client thread. Reads and writes belong on a background
  thread, marshalling back via `clientThread.invoke()` only if the client is touched.
- **Imported files are untrusted input.** Malformed JSON, absent fields, refs that do not
  parse, coordinates outside the world — all must fail gracefully with a message rather
  than throwing. Never let a bad import wipe the existing list.
- A schema version field in the file costs nothing now and avoids a painful migration later.

---

## D15. Saved-location schema — coordinates only, refs computed live

**Decision: store the raw world point. The grid reference is recomputed from the
viewer's own settings every time it is displayed, and is never persisted.**

This resolves O11.

```json
{ "label": "Wine of Zamorak", "x": 3095, "y": 3494, "plane": 0 }
```

```
You (size 64) see:      L35
Friend (size 32) sees:  W69
```

Same physical spot, both correct.

**Why this is right.** The grid reference is a *display layer over* a location, not the
location itself. Treating it as such means a shared file always points at the right
physical place regardless of what grid size either player runs — import can never
silently mislead. Combined with D1's permanently-fixed anchor, the only variable left is
grid size, and this schema makes even that irrelevant to correctness.

**Consequences**

- **Refs are derived, never stored.** There is exactly one function turning a `WorldPoint`
  into a ref, used by the HUD, the ground label, the world map and the panel alike. No
  possibility of a stored ref drifting out of sync with the settings that produced it.
- **Changing grid size relabels every saved location instantly**, with no migration, no
  rewrite, and no stale data. Grid size becomes a pure view setting.
- Search works the other way — ref text to `WorldPoint` — so the ref parser and formatter
  must round-trip exactly. Worth a property test: format then parse should return the
  origin tile of the same cell for every cell in the world, at several grid sizes.
- A ref identifies a *square*, not a tile, so parsing `L35` yields a cell, and pinning it
  should target that cell's centre (or origin) deterministically. Saving your current
  location stores your **exact tile**, which is strictly more precise than the ref you see
  — worth remembering: what you save is finer-grained than what is displayed.
- **The file is less hand-writable.** You cannot type `"ref": "L35"` into it; you need
  coordinates. Acceptable, since Save-current-location and Import are the intended routes.
  A future convenience could accept a ref field on *import only*, resolving it against the
  importer's grid size, but that reintroduces exactly the ambiguity this decision removes —
  so it should be an explicit, labelled feature or not done at all.
- `plane` is stored, matching D3's `+1`/`+2` suffix handling.

**Declined:** storing the ref and grid size alongside the coordinates, which would have
let the panel show "saved as L35 @64". Rejected as noise — with coordinates as the source
of truth, the original author's grid size has no bearing on correctness and telling the
user about it invites confusion rather than preventing it.

---

## D16. Import behaviour — ask each time

**Decision: importing shows a dialog offering Merge or Replace, with counts.**

```
Import locations.json

  This file has 12 locations.
  You have 4 saved.

  ( ) Merge   - keep mine, add new
  ( ) Replace - discard mine

        [Cancel]  [Import]
```

**Rationale.** Merge is the common case, but Replace is genuinely useful when adopting a
clan's canonical set wholesale, and guessing wrong in either direction is annoying —
silently duplicating entries, or silently destroying someone's own saves. One extra click
per import is a fair price, and imports are rare.

**Consequences**

- **Showing the counts before acting is the point of the dialog**, not decoration. "This
  file has 12, you have 4" is what makes Replace a safe thing to offer at all — the user
  can see exactly what they are about to discard.
- **Replace is destructive and needs a safety net.** Write a timestamped backup of the
  current file before overwriting (`locations.backup-<timestamp>.json` in the same
  directory), so a mis-click is recoverable. Cheap insurance.
- **Merge needs a duplicate rule.** Match on coordinates — `(x, y, plane)` — since under
  D15 that is the identity of a location; labels are just names for it. Two entries at the
  same tile are the same place regardless of what each calls it.
  - Still open: on a coordinate collision during merge, whose **label** wins? Keeping the
    existing one is the least surprising default. Logged as O16.
- **Report the outcome.** "Imported 8 new, skipped 4 already present" — an import that
  silently appears to do nothing (because everything was a duplicate) otherwise reads as
  a bug.
- Validate before mutating anything: parse the whole file, reject it wholesale on
  malformed JSON or out-of-range coordinates, and only then apply. A partial import that
  half-succeeded is the worst outcome.
- The dialog is Swing, shown from the panel on the EDT — no client-thread involvement.

---

## D17. Pin lifecycle — per-location toggle

**Decision: each saved location has its own pin toggle. The list is your library; the
pinned subset is what appears on the map.**

This resolves O14.

```
SAVED
  Wine spot     [P][map][x]
  Clan meet     [ ][map][x]
  Boss camp     [P][map][x]
  Bank rdv      [ ][map][x]

[World Map]
   * Wine spot
        * Boss camp
```

Keep thirty locations saved, show the four relevant to tonight.

**Rationale.** Scales to imported clan sets, which is precisely where "pin everything"
falls over — a 40-entry shared file would carpet the map. It also gives the panel a real
job beyond storage: curating what you are currently working with.

**Consequences**

- **Pinned state is per-user, not per-location-in-the-file.** It must be stored, so the
  D15 schema gains a boolean:

  ```json
  { "label": "Wine of Zamorak", "x": 3095, "y": 3494, "plane": 0, "pinned": true }
  ```

  Open question: does that flag travel through export/import, or is it local-only? If it
  travels, a clan file arrives with someone else's idea of what matters; if it does not,
  a shared "tonight's route" file loses its whole point. Logged as O17.
- **Imported entries should default to unpinned** if the flag is local-only, so importing
  a large set never floods the map without the user asking.
- The panel needs a **"clear all pins"** action once several can be lit at once, otherwise
  turning off a dozen is tedious.
- Interaction with D12's search pin: a searched ref produces a *transient* pin that is not
  a saved location. It should be visually distinct from saved pins, and there needs to be
  an obvious way to dismiss it or promote it into a saved location. Logged as O18.

**Implementation.** One `WorldMapPoint` per pinned entry, added/removed through
`WorldMapPointManager` as toggles flip. Give them a marker subclass so `removeIf()` can
clear exactly this plugin's pins in `shutDown()` without disturbing other plugins'.
Toggling happens on the Swing EDT while `WorldMapPointManager` is touched from the client
side — marshal with `clientThread.invoke()`.

Set `snapToEdge` on saved pins so an off-screen pinned location still indicates its
direction from the current view, and set `tooltip` to the label plus the live ref.

---

## D18. v1 scope — everything ships together

**Decision: one release containing the full feature set.** This resolves O13.

```
v1 ships:
  grid lines (scene + world map)
  HUD ref readout + ground label
  N/S/E/W compass labels
  instance handling
  ref search + map pin
  side panel
  saved locations JSON
  import / export
```

**Accepted trade-off: this is the longest build before anything is testable in-game, and
the most to get wrong at once.** A staged alternative was offered and declined.

**What this means in practice**

- **Sequence the build so the core is exercisable early even though it ships late.** The
  grid maths, ref formatting and scene overlay should be working and verified in-game long
  before the panel exists. Nothing about shipping together requires building together.
- **The ref format and anchor become permanent at v1.** With saved files in the wild from
  day one, `../world-grid-plugin/OPEN-QUESTIONS.md` O1 (the exact anchor constant) is
  locked at first release rather than after a proving period. It cannot be revisited later
  without invalidating every file anyone has exported. **Get O1 right before v1, not
  during it.**
- **The JSON schema is likewise permanent at v1** — D15's coordinate fields plus D17's
  `pinned` flag. Include a schema version field from the very first write (D14) so a later
  change is a migration rather than a breakage.
- One plugin-hub review round rather than three. Review is not instant, so this is a real
  saving, but it also means the first external feedback arrives on the complete surface.

**Testing burden.** Per `RESEARCH.md` §4, none of this can be verified by an AI agent —
RuneLite's guidelines forbid automating game input. Everything above is manual in-game
verification, and shipping it all at once concentrates that into a single pass. Budget for
it: grid rendering at several sizes, compass labels under camera rotation, instance
suppression, search and pin behaviour, and a full import/export round-trip between two
clients on *different* grid sizes.

---

## D19. Search input — right-click the world map

**Decision: a "Search grid ref" entry on the world map's right-click menu, opening an
in-game text prompt.** This resolves O10.

```
[World Map]  right-click

  Search grid ref
  Focus on  Wine spot
  Cancel

-> opens an in-game text prompt
```

**Rationale.** Discoverable at the moment of need — you find it while looking at the map,
which is exactly when you want to search. It also sits alongside core RuneLite's own
"Focus on" entry, so it reads as part of the map rather than a bolted-on plugin feature.

**Declined:** a search box in the side panel, and a `::grid` chat command. Both remain
easy to add later — the panel field especially, since it would reuse the same parser and
pin logic — but neither ships in v1 unless revisited.

**Consequences**

- **The panel is now purely a library of saved locations**, not a search interface. That
  simplifies its layout: list, per-entry pin/map/delete controls, save-current, import,
  export. No text field, no live-resolution display.
- **Searching and saving are separated.** With no panel search box, promoting a searched
  ref into a saved location needs an explicit affordance — otherwise the only route to a
  saved entry is "stand there and press save current location", which defeats the purpose
  of searching for a place you are not at. This makes O18 more pressing, not less.
- The prompt is the only place a ref is typed, so its validation carries the whole burden
  of rejecting bad input clearly.

**Implementation**

- Menu entry via `client.createMenuEntry(-1)` with `MenuAction.RUNELITE` — client-side
  only, nothing sent to the server, so it is clear of the "no menu entries that cause
  actions to be sent to the server" rule. Core's `WorldMapOverlay` builds its "Focus on"
  entry exactly this way, attached through a timer listener on a map widget.
- Prompt via `ChatboxPanelManager.openTextInput("Grid ref:")`, using:
  - `.addCharValidator(...)` to restrict input to letters, digits and the plane suffix
    characters, so malformed refs are difficult to type in the first place;
  - `.onChanged(...)` for live feedback if wanted;
  - `.onDone(...)` to parse, then jump and pin.
- On success: `setWorldMapPositionTarget(cell)` plus a transient `WorldMapPoint`.
  On failure: a clear message naming what was wrong, not a silent no-op.
- The prompt runs on the client thread; no EDT marshalling needed on this path, unlike the
  panel's buttons.

---

## D20. `pinned` travels through export, but always imports as unpinned

**Decision: the `pinned` flag is part of the file schema and is written on export, but
import unconditionally sets it to `false`.** This resolves O17.

```json
{ "label": "Wine spot", "x": 3095, "y": 3494, "plane": 0, "pinned": true }
```

```
  file says pinned: true
         |
     [import]
         v
  your list: pinned = false   (always)
```

**Rationale.** Pinning is a statement about *your* current session, not a property of the
place, so nobody else's curation should light up your map. Importing a 40-entry clan file
can therefore never carpet the world map, which is the failure mode D17 was designed to
avoid in the first place. Keeping the field in the file anyway means the schema is
complete and round-trippable from day one — D18 makes it permanent, and a field that
exists but is ignored on read is far cheaper than one that has to be added later.

**Consequences**

- **Export is lossless; import is deliberately lossy.** Export-then-import is *not* an
  identity operation on the `pinned` field, and that is intentional. Anyone reading the
  file can see what the author had lit up; the client just declines to act on it.
- **The field is never trusted from disk.** Read it, discard it, write `false`. This also
  means a malformed or missing `pinned` needs no special handling on import — it is
  overwritten regardless, which removes a whole class of validation from D14's
  untrusted-input surface.
- The plugin's own working file at `.runelite/world-grid/locations.json` **does** honour
  the flag on load — it is your own state, not an import. Only the `JFileChooser` import
  path clears it.
- Cost: a "tonight's route" file does not arrive pre-lit. The recipient imports, then pins
  the four entries they care about. Accepted — D22's pinned-only export makes such a file
  short enough that this is a few clicks, not a chore.

---

## D21. Promoting a searched ref into a saved location — right-click the pin

**Decision: the transient search pin carries a "Save location" menu entry, which opens a
chatbox name prompt and writes the entry.** This resolves O18.

```
[World Map]  right-click the * pin

  Save location
  Remove pin
  Cancel

-> ChatboxPanelManager: "Name:"
-> writes { label, x, y, plane, pinned: false }
```

**Rationale.** It closes the gap D19 opened: with search living on the map and the panel
reduced to a pure library, there was no route from "I searched GA12" to "GA12 is in my
list" short of travelling there. Putting the affordance on the pin keeps the whole
search-and-save flow in one place — you are already looking at the map, at the thing you
want to keep — and it reuses D19's existing menu-entry and text-prompt machinery rather
than introducing a new interaction model.

**Consequences**

- **The transient pin needs its own right-click handling**, distinct from saved pins. A
  marker subclass already exists per D17's `removeIf()` requirement; the transient pin
  gets its own so the menu can differ (saved pins do not offer "Save location").
- **It must look distinct from saved pins**, otherwise "is this saved yet?" is unanswerable
  at a glance. Different icon or colour, decided at build time.
- **A second search moves the existing transient pin rather than adding another.** There is
  at most one transient pin at any moment. This keeps "Remove pin" unambiguous and avoids
  accumulating clutter across a session of searching.
- **The transient pin does not survive `shutDown()` or a client restart** — it is not
  saved state, and clearing it is already covered by the marker-subclass sweep.
- Saving from a searched ref stores the **cell's deterministic origin or centre tile**
  (per D15), not an exact tile — unlike save-current-location, which stores where you
  actually stand. Both are valid `WorldPoint`s; the saved entry is simply coarser. Worth
  noting in case a saved-from-search entry later reads as "slightly off".
- The prompt runs on the client thread; the subsequent file write must not. Marshal to a
  background thread for the save, per D14.

---

## D22. Export asks: all saved, or pinned only

**Decision: Export opens a small dialog offering the whole list or the pinned subset,
with counts, before the `JFileChooser`.** This resolves O19.

```
Export locations

  ( ) All saved      (30)
  ( ) Pinned only     (4)

     [Cancel]  [Export]

  -> world-grid-locations.json
  -> world-grid-pinned.json
```

**Rationale.** The two exports serve genuinely different purposes and neither subsumes the
other: "here is our clan's canonical set" wants everything, "here is tonight's route" wants
the four you have lit up. Forcing one to be built by hand-editing JSON or by temporarily
deleting entries would be worse than one extra dialog on a rare action — the same argument
D16 already accepted for Merge/Replace on import.

**Consequences**

- **Showing counts is the point**, matching D16. "All saved (30) / Pinned only (4)" tells
  the user what each option will actually produce before they commit.
- **The default filename follows the choice** — `world-grid-locations.json` for all,
  `world-grid-pinned.json` for the subset — so two exports do not silently overwrite each
  other in the `JFileChooser`.
- **Default directory:** the user's home, or the last directory used for import/export,
  remembered in config. Not `RuneLite.RUNELITE_DIR` — an exported file is meant to be
  found and sent to someone.
- **"Pinned only" is disabled when nothing is pinned**, with the count reading `(0)`,
  rather than producing an empty file.
- Both forms write the identical schema, including the `pinned` field per D20. A
  pinned-only export is a filtered list, not a different format.
- The dialog is Swing on the EDT, like D16's.

---

## D23. Merge collisions keep the existing label

**Decision: on a coordinate collision during Merge, the label already in your list wins.
The incoming label is discarded.** This resolves O16.

```
  mine:     "my wine spot"    (3095, 3494, 0)
  incoming: "Wine of Zamorak" (3095, 3494, 0)

  result:   "my wine spot"

  "Imported 8 new, skipped 4 already present"
```

**Rationale.** Merge's contract, as stated in D16's own dialog, is *"keep mine, add new"* —
silently renaming entries the user named themselves would violate the thing the dialog
promised. A user who genuinely wants the clan's naming can choose Replace, which is exactly
what that option is for. The least-surprising default is also the one that makes Merge
safe enough to be the common case.

**Consequences**

- **Merge never mutates an existing entry at all** — not its label, not its `pinned` state
  (which D20 forces to `false` on import anyway, for new entries only). It is purely
  additive. This makes it trivially safe and needs no backup file, unlike Replace.
- **The outcome report must name the skips**, per D16: "Imported 8 new, skipped 4 already
  present". An import where every entry collided would otherwise look like a silent no-op.
- **Identity remains `(x, y, plane)`**, per D16 — two entries at the same tile are the same
  place whatever each calls it. Labels are never part of the match.
- Cost: adopting a clan's agreed names for places you had already saved under your own
  names requires Replace, or deleting your versions first. Accepted — that is a rare,
  deliberate act, and Replace with its timestamped backup covers it.

---

## D24. Bulk deletion — row multi-select

**Decision: saved rows carry a selection checkbox, with a "Delete selected (n)" action.**

```
SAVED
 [x] Wine spot   [P][map]
 [ ] Clan meet   [ ][map]
 [x] Boss camp   [P][map]

  [ Delete selected (2) ]
```

**Rationale.** One mechanism covers both jobs — clearing the list entirely (select all,
delete) and pruning a handful of stale entries — where a dedicated "Clear all" button only
covers the first and leaves the more common case as repeated single-row deletion. It also
avoids adding a large destructive button to a panel where the surrounding controls are all
per-row and reversible.

**Consequences**

- **The panel needs a select-all control**, otherwise "delete everything" is 30 clicks and
  the decision fails at the job it was chosen for. A header checkbox is the natural fit.
- **Deleting more than a handful of rows confirms first**, naming the count, and writes a
  timestamped backup alongside D16's Replace backup — same file convention
  (`locations.backup-<timestamp>.json`), same directory, so recovery works identically
  however the data was lost.
- **Selection is transient view state**, never persisted and never written to the file. It
  clears on panel close, on import, and after a delete.
- **Selection is orthogonal to pinning.** Two independent per-row booleans sit next to each
  other in the UI, so they must be visually distinguishable — the pin toggle is a lit
  state, the checkbox is a selection. Getting this wrong makes the panel unreadable.
- Deleting a pinned entry removes its `WorldMapPoint` too, through the same
  `WorldMapPointManager` path D17 established.
- **"Clear all pins" is a separate, non-destructive concern** and is not covered by this
  decision. D17 flagged it as still wanted; it remains so.

---

## D25. No hard cap on the saved list — warn on large imports

**Decision: the list is unbounded. Import warns before proceeding when the incoming file
holds more than 200 locations.** This resolves O20.

```
Import 812 locations?

  Large lists may slow the
  panel and world map.

     [Cancel]  [Import]
```

**Rationale.** A hard limit risks rejecting a legitimate large clan set outright, and any
number chosen now would be a guess made before a single line of rendering code exists. A
warning keeps the user informed without the plugin deciding on their behalf what is too
much.

**Consequences**

- **This accepts the risk D14 flagged** — unbounded untrusted JSON becoming Swing rows and
  map pins. The mitigation is the warning plus D20: imported entries are always unpinned,
  so even an 800-entry import adds *zero* map pins until the user starts lighting them up.
  The panel list is the only thing that grows, and it is the cheaper of the two surfaces.
- **The warning stacks with D16's Merge/Replace dialog**, which already shows counts. These
  should be one dialog with an added caution line, not two dialogs in sequence.
- **Revisit if the panel proves slow.** If in-game testing (D18) shows the list degrading
  well under 200, the threshold moves or becomes a real cap. Logged as a build-time check
  rather than a design question.
- The 200 threshold is a warning point only — nothing in the code should treat it as a
  limit, and no operation may fail because of it.

**Declined:** a 500-entry cap, and separate caps for saved entries and pins.

---

## D26. The HUD ref readout has its own toggle

**Decision: the HUD grid-ref readout is controlled independently of D5's two render
checkboxes.** This resolves O3.

```
config:
  [ ] Draw grid on world map
  [ ] Draw grid in scene
  [x] Show grid ref readout

     ->  L35  still shown
```

**Rationale.** "Where am I, in terms both of us understand" is the plugin's actual purpose,
and it does not require lines drawn over the game world to answer. Gating the readout on a
render surface would mean anyone who finds the grid visually noisy loses the feature
entirely — which is backwards, since the readout is the cheapest and most useful part.

**Consequences**

- **Three independent toggles**, and all three off is a valid state in which the plugin
  draws nothing. It should behave correctly rather than being treated as "disabled".
- **The ref computation runs whenever any of the three is on**, so it sits below all of
  them rather than inside the scene overlay. One function, per D15.
- **The ground label stays gated on the scene grid checkbox.** It is drawn in the world, at
  a position that only makes sense with the square outlined, so it belongs to the scene
  render rather than to the readout. An independent ground-label toggle was offered and
  declined — the HUD readout already covers "tell me my ref without drawing the grid".
- **The world map does not highlight your current cell.** Offered under O8 and declined:
  the HUD readout answers "where am I" without needing the map open, and tinting a cell
  adds a fourth thing to configure for a question already answered.
- D10 still applies to the readout independently — in an instance it shows `—`, whatever
  the render toggles say.

---

## D27. Label thresholds close the remaining rendering questions

**Decision: scene labels are bounded by a line budget, world map labels by a zoom
threshold. Both are single constants, tuned in-game.** This resolves O5 and O6.

```
scene:      24 boundary lines per axis        (draws closer in when a small
                                               grid size would exceed it)
world map:   3 pixels per cell -> draw lines
            30 pixels per cell -> draw labels
```

**Rationale.** O5 and O6 both asked "at what point does this become unreadable", which is
a question about pixels on a screen rather than about design. Picking a starting value and
making it trivial to change beats leaving the question open, because neither can be
answered without looking at it in-game — and once you are looking, changing a constant is
the cheapest possible fix.

**Consequences**

- **These are safe starting values, not measured ones.** They were chosen to err toward
  drawing too little rather than too much. Expect to tune them after the first session.
- **The scene budget is a correctness matter, not only taste.** `render()` runs every
  frame, and without a cap a grid size of 1 asks for over twenty thousand projections per
  frame. The constant is what keeps the overlay off the frame-rate budget.
- The world map keeps two separate thresholds because labels need far more room than
  lines, and showing many cells' references at once is the point of the map.

**Also closed by the build**

- **O7 — row-letter direction: confirmed.** Letters encode rows, numbers encode columns.
  Five landmarks reproduce exactly under this reading and no other. Pinned by tests.
- **O21 — parser grammar: implemented and tested.** Case-insensitive, tolerates spaces,
  accepts the plane suffix, rejects malformed input, column zero, planes outside 1-3 and
  references outside the world. Round-trip with the formatter is pinned by a property
  test across the world at six grid sizes.
- **O2 — grid size mismatch: accepted, unmitigated.** Two players on 63 and 64 disagree
  silently and nothing in the code can detect it. D15 removes the risk from saved files;
  spoken references remain exposed. Revisit only if it bites.
- **O4 — underground marker: accepted.** D4's decline stands; no `~` prefix.
- **O9 — plugin name: a pre-submission task.** "World Grid" is the working name. Checking
  the hub for collisions needs a GitHub account and a plugin-hub PR, which is a manual
  step regardless.
