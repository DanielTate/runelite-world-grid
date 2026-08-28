# World Grid

A labelled grid over Old School RuneScape so two players can name an exact place to each
other. You are standing in `W39`; so is your friend, and you both know it.

```
"Meet me at W39."          ground level
"I'm at W39+2."            two floors up
```

References are computed from a fixed anchor, so the only setting both players must match
is the grid size.

## Install

Not on the plugin hub yet. To build and run locally:

```
./gradlew build     # compile and test
./gradlew run       # launch a development client with the plugin loaded
```

## What it does

| | |
|---|---|
| **Grid lines** | On the world map and on the game scene. |
| **Your reference** | A movable readout, plus the reference labelled on the square you stand in with N/S/E/W markers on its edges. |
| **Search** | Right-click the world map → *Search grid ref*. Type `W39`, the map jumps there and drops a pin. |
| **Saved locations** | A side panel of named places. Pin the ones you care about to show them on the map. |
| **Import / export** | Share a list of named places with your clan as a JSON file. |

Inside an instance (raids, Theatre of Blood, a POH) the plugin shows `—` and draws
nothing. Instanced coordinates do not match where you actually are, so a reference there
would be confidently wrong.

## Settings

| Setting | Default | |
|---|---|---|
| Grid size | 64 | Tiles per square, 1–256. **Both players must match this.** |
| Draw grid on world map | on | |
| Draw grid in scene | on | |
| Show grid reference readout | on | Independent of the two above. |
| Grid colour | white, semi-transparent | |
| Line thickness | 1 | |

## Files

Your saved locations live in `.runelite/world-grid/locations.json`.

```json
{
  "version": 1,
  "locations": [
    { "label": "Wine of Zamorak", "x": 3095, "y": 3494, "plane": 0, "pinned": true }
  ]
}
```

Only the coordinate is stored — the reference is recomputed from *your* grid size every
time it is shown, so a file shared between two players on different sizes still points at
the same physical place.

- **`pinned` is written on export but always cleared on import.** Nobody else's curation
  lights up your map.
- **Merge** keeps your entries and adds the new ones; two entries at the same tile are the
  same place, and your label wins. **Replace** discards yours and writes a timestamped
  backup (`locations.backup-<timestamp>.json`) first, as does any delete.

## Testing

The pure logic — grid maths, reference formatting and parsing, the file format, import
planning, and every edit the panel can make — is covered by unit tests. Rendering, the
panel and the map menu cannot be tested automatically: **verifying those in-game is the
user's job.**

## Licence

BSD-2-Clause. See `LICENSE`.
