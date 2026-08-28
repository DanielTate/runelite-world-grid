# World Grid Plugin — Open Questions

Unresolved. Each needs an answer before or during build.

**Resolved and moved to `DECISIONS.md`:** O3 (HUD readout toggle -> D26), O8 (current-cell
highlight -> declined in D26), O10 (search input -> D19), O11 (saved schema -> D15), O13
(v1 scope -> D18), O14 (pin lifecycle -> D17), O16 (merge labels -> D23), O17 (pinned
export -> D20), O18 (search -> save -> D21), O19 (export scope -> D22), O20 (size cap ->
D25).

---

**O1. Pin down the exact anchor constant. — BLOCKING, decide before v1**

D1 fixed the anchor at the south-west corner of the overworld and used `(1024, 2496)` for
its worked examples, but that value is **not yet verified** as the true SW extent of the
playable surface.

D18 raises the stakes: with everything shipping in v1, exported files exist in the wild
from day one. Changing the anchor afterwards renames every square in the world and
invalidates every reference anyone has written down or shared. Worth deriving from actual
map data rather than picking a round number.

This is now the **only remaining blocker**. The schema questions that shared its urgency
(O17, O18, O19) are settled, so the v1 file format is fixed — see D20's field list. The
anchor is the last permanent thing still undecided.

**O2. Is a bare ref enough, given grid size is free-form?**

D2 allows any size 1-256 with no mismatch protection, and declined stamping the size onto
refs (`L35 /64`). Two players on 63 and 64 disagree silently. Note D15 removes this risk
from *saved files* (coordinates are the source of truth) but not from **spoken refs**,
which is the plugin's primary use case. Revisit if it bites.

**O4. Are underground refs confusing enough to need a marker?**

D4 emits normal refs underground, pointing at real coordinates that appear nowhere on the
surface map. A `~` prefix (`~D88`) was offered and declined. `Constants.OVERWORLD_MAX_Y`
(4160) is the cheap test if added later.

**O5. Label placement and legibility at small grid sizes.**

D6 and D7 put the cell ref mid-square and four direction labels on its edge midpoints. At
size 22 those five sit close together, and at extreme camera pitch the far edge label
crowds the near one. May need a minimum grid size below which labels are suppressed.

Note D26 keeps the ground label gated on the scene-grid toggle, so anyone who finds this
crowded has the HUD readout as an out — which lowers the stakes but does not remove the
question.

**O6. World map label density and zoom thresholds.**

D8 limits labels to the current cell **in the scene**; the world map is a different
problem, where showing many cells' refs is the point. Needs its own threshold. The
`gridSize * pixelsPerTile < 3` line bail-out is established; the label threshold is not.

**O7. Row-letter direction — confirm in-game.**

D7 establishes +y is north, so row A is southernmost and letters increase going north,
numbers going east. Sanity-check against the samples (Lumbridge L35, Kourend Castle T4).

**O9. Plugin name.**

"GridScape" is taken by an unrelated hub plugin. "World Grid" is the working name; check
the hub for collisions before submitting.

---

*From the JSON storage / import-export expansion (D14-D26).*

**O21. What exactly does the ref parser accept?**

D19 makes the map prompt the only place a ref is typed, so its validation carries the full
burden. Case sensitivity (`ga12` vs `GA12`), whitespace, the plane suffix (`L35+2`),
out-of-range refs for the current grid size, and refs pointing outside the playable world
all need defined behaviour and clear error messages.

Per D15 the formatter and parser must round-trip exactly — worth a property test across
every cell at several grid sizes.

D21 adds a wrinkle: a searched ref that becomes a saved location stores the cell's origin
or centre tile, so "which tile does a parsed ref resolve to" is now a persisted decision,
not just a camera target. Pick one and use it everywhere.

---

## Build-time checks (not design questions)

- **D25's 200-location warning threshold.** No hard cap was set. If in-game testing shows
  the Swing list or the map degrading well below 200, the threshold moves or becomes a
  real cap.
- **D24's confirm threshold.** "More than a handful" of selected rows triggers a confirm
  dialog — pick the actual number when the panel exists.
- **D21's transient-pin styling.** It must be visually distinct from saved pins; the icon
  or colour is a build-time call.
- **D22's remembered export directory.** Where the last-used path is stored in config.
