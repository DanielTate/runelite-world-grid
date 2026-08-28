# World Grid Plugin — Open Questions

Unresolved. Each needs an answer before or during build.

**Resolved and moved to `DECISIONS.md`:** O1 (anchor -> D1), O2/O4 (accepted, see below),
O5/O6/O7/O9/O21 (-> D27), O3 (HUD readout toggle -> D26), O8 (current-cell
highlight -> declined in D26), O10 (search input -> D19), O11 (saved schema -> D15), O13
(v1 scope -> D18), O14 (pin lifecycle -> D17), O16 (merge labels -> D23), O17 (pinned
export -> D20), O18 (search -> save -> D21), O19 (export scope -> D22), O20 (size cap ->
D25).

---

*(O1, the anchor, is resolved. See D1 — it moved to `(768, 1792)` after Sailing pushed
the playable surface south and west of the originally scoped value. There is no longer a
blocking question.)*

**O2. Is a bare ref enough, given grid size is free-form? — ACCEPTED, unmitigated**

D2 allows any size 1-256 with no mismatch protection, and declined stamping the size onto
refs (`L35 /64`). Two players on 63 and 64 disagree silently. Note D15 removes this risk
from *saved files* (coordinates are the source of truth) but not from **spoken refs**,
which is the plugin's primary use case. Revisit if it bites.

**O4. Are underground refs confusing enough to need a marker? — ACCEPTED, no marker**

D4 emits normal refs underground, pointing at real coordinates that appear nowhere on the
surface map. A `~` prefix (`~D88`) was offered and declined. `Constants.OVERWORLD_MAX_Y`
(4160) is the cheap test if added later.




**O9. Plugin name. — pre-submission task**

"GridScape" is taken by an unrelated hub plugin. "World Grid" is the working name; check
the hub for collisions before submitting.

---

*From the JSON storage / import-export expansion (D14-D26).*


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
