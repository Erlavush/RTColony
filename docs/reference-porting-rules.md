# Reference Porting Rules

RTColony should use the reference mods as the design source of truth.

- Do not invent a custom HUD style when an equivalent Reign of Nether, MineFortress, or
  MineColonies pattern exists.
- Prefer copying or closely porting the reference implementation when the license allows
  it, then adapt only what is necessary for NeoForge 1.21.1 and RTColony's package/API
  boundaries.
- Keep copied assets and adapted code attributed in `THIRD_PARTY_NOTICES.md`.
- For Reign of Nether RTS UI, use its frame textures, portrait rendering, health bars,
  icon layout, and selection HUD behavior as the visual target.
- For MineFortress camera/selection behavior, use its RTS control feel as the target when
  it is compatible with the current camera implementation.
- For MineColonies integration, use MineColonies' own colony/citizen/building/request
  APIs and UI conventions instead of recreating colony concepts independently.
- If a reference subsystem is too large to copy directly, port the smallest coherent
  class/function group and document which reference files it came from.
- Do not mix incompatible code or assets. Recheck the local reference license before
  copying from any new source.
