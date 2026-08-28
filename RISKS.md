# Risks, Issues and Concerns

Live record kept during the build. Newest first within each section.

## Open

**R5. The anchor's margin is a bet on how far the map expands next.**

The shipped anchor `(768, 1792)` sits three regions west and four south of the current
south-west-most surface square `(960, 2048)`. That is deliberate insurance, not a
measurement -- the map has moved south-west three times in a decade and the constant
cannot be corrected after release.

If a future expansion overshoots the margin, references break permanently for content
beyond it. Nothing in the code can prevent that; the only mitigation available was to
buy more margin, at the cost of longer references.

**Watch for:** any content update that extends the surface west of x=768 or south of
y=1792. `LandmarkReferenceTest.theSouthWestMostSurfaceSquareIsInsideTheGrid` is the
canary, but it only checks the corner known at build time.

---

**R6. `Constants.OVERWORLD_MAX_Y` is stale and must never be used as a bound.**

Sailing's northern oceans reach y=4223, above the constant's 4160. Any `y < OVERWORLD_MAX_Y`
test now wrongly rejects the northernmost band of playable surface.

The plugin deliberately does not use it -- validation runs against the full 0-16383 world
space, which stays correct however the map grows. Recorded here because it is a plausible
thing for someone to add later, and `RESEARCH.md` still cites the constant.

---

**R3. Three documented landmark samples did not match. Partly explained.**

D1 lists eight worked examples. Five reproduce exactly under anchor (1024, 2496) at grid
size 64, using coordinates recalled independently of the samples:

```
Lumbridge       (3222,3218)  L35  MATCH
Varrock square  (3213,3428)  O35  MATCH
Grand Exchange  (3165,3489)  P34  MATCH
Falador         (2965,3380)  N31  MATCH
Edgeville       (3087,3500)  P33  MATCH
Catherby        (2810,3445)  M26  got O28
Prifddinas      (2240,3280)  J23  got M20
Kourend Castle  (1610,3670)  T4   got S10
```

Now partly explained. **Kourend Castle's documented `T4` was simply wrong**: no anchor at
any grid size 1-256 produces both `L35` for Lumbridge and `T4` for Kourend Castle, and the
real coordinate `(1628, 3673)` gave `S10` under the old anchor. It reads like a
plausible-sounding label attached to the wrong landmark.

`Catherby M26` and `Prifddinas J23` remain unexplained and are **not** pinned as tests.
They are cosmetic now -- every sample in D1 was rewritten for the new anchor -- but if
either turns out to be right it would imply something still misunderstood about the
convention. Low priority.

Note the whole exercise was still worth it: the five matches confirmed the *convention*
(letters encode rows), which is what let the anchor move safely.

---

**R4. `OPEN-QUESTIONS.md` O2 is unmitigated and now shipped into code.**

Grid size is free-form 1-256 with no mismatch protection, so two players on sizes 63 and
64 disagree silently and neither can tell. D15 removes this risk from *saved files*
(coordinates are the source of truth) but not from **spoken references**, which are the
plugin's primary use case.

Nothing in the code can detect this. Worth revisiting if it bites in practice.

## Resolved

**R2. The reference convention was ambiguous between two readings.** D7 states letters
increase going north and numbers going east, but that could be read as letters encoding
either axis. Resolved by computing both against the documented samples: **letters encode
the row (north-south), numbers encode the column (east-west)**, so ref = letters(row) +
(column + 1). Only this reading reproduces the documented samples. Now pinned by tests.

**R1. The anchor value looked like a round-number guess.** `OPEN-QUESTIONS.md` O1 flagged
(1024, 2496) as unverified. Two independent facts now support it:

- `2496 + 26 * 64 = 4160`, which is exactly `Constants.OVERWORLD_MAX_Y`. The grid's 26
  rows span the overworld's full height precisely -- that is why every surface reference
  fits in a single letter, and it is not a coincidence a guess would produce.
- Both coordinates are region-aligned (1024 = 16 x 64, 2496 = 39 x 64).

Downgraded from blocking to R3 above, which is now only about confirming three sample
coordinates.

## Accepted

**A1. Underground references point at coordinates that appear nowhere on the surface map.**
D4 declined a marker prefix. `Constants.OVERWORLD_MAX_Y` is the cheap test if this is ever
revisited. Accepted as designed.

**A2. No hard cap on the saved-locations list.** D25 warns above 200 rather than
refusing. The mitigation is D20: imported entries always arrive unpinned, so even a large
import adds zero map pins and only grows the panel list. Revisit if the panel proves slow
in-game.
