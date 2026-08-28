# Risks, Issues and Concerns

Live record kept during the build. Newest first within each section.

## Open

**R3. Three documented landmark samples do not match their documented references.**

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

Five independent matches is strong evidence the anchor and the reference convention are
both right. The three misses are most likely wrong *coordinates* on my side rather than a
wrong anchor -- they are the landmarks whose positions are least reliably recalled -- but
that is unconfirmed. Being checked now.

Only the five verified landmarks are pinned in `LandmarkReferenceTest`. The other three
are deliberately excluded rather than pinned to values that may be wrong.

**Impact if the anchor is wrong:** every square in the world is renamed and every saved
or spoken reference becomes invalid. This is the one value that cannot be fixed after
release.

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
