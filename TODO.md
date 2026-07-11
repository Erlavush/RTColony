# RTColony Product Backlog

This is a concise, player-facing feature backlog. Treat it as direction, not permission to
implement a feature without the alignment questions and an approved design required by
`AGENTS.md`.

## Confirmed Direction

- RTColony is an RTS control layer over MineColonies and Create. Final gameplay actions
  must remain server-authoritative through the owning mod's supported APIs/messages.
- Prefer one coherent RTS workflow over many disconnected screens:
  comfortable camera -> select/focus -> understand -> plan -> take permitted action.
- Follow Reign of Nether, MineFortress, MineColonies, Structurize, Xaero, and JourneyMap
  reference patterns where compatible; check APIs/licenses before integration work.

## Priority 1: Camera Comfort and View Modes

- Reduce motion sickness from current terrain-following camera movement.
- Add selectable RTS camera modes:
  - Stabilized perspective: terrain follow is reduced/clamped or optional.
  - Free perspective: preserve current RTS behavior.
  - True isometric: orthographic, fixed-angle RTS view based on the Reign of Nether
    reference implementation.
- Expose terrain-follow strength, smoothing, fixed-height behavior, movement speed, and
  camera mode in client config/keybindings.
- Keep input, cursor raycasting, zoom, selection, and placement accurate in every mode.
- Design detail still needed: decide whether true isometric is strictly orthographic or
  retains any perspective; do not implement until approved.

## Priority 2: Smart Selection and Camera Follow (Implemented)

- [x] Selecting an entity or citizen offers camera follow. Manual camera movement, `Esc`,
  or entering build placement exits follow cleanly.
- [x] A selected/followed entity receives a target-aware clean terrain cutaway
  only while genuinely obstructed. The cutaway tracks the projected entity
  bounds, avoids full-area dithering, and closes when line of sight is restored.
- [x] Loaded MineColonies buildings are selectable across their schematic area, highlight
  their full bounds, and show the RTS building information panel.
- [x] Citizen and building panels appear automatically on selection and offer a button to
  open the native MineColonies detail screen.
- Controlled citizen/building actions remain intentionally gated until each action has a
  supported, permission-safe, server-authoritative MineColonies integration path.

## Priority 3: Town Hall Colony Command Screen

- Build a unified Town Hall/Colony Command screen rather than separate overview UIs.
- Include tabs/sections for colony status, citizens, buildings, work orders, resources,
  food, housing, and happiness.
- Citizen and building directories need search/filtering plus click-to-focus/select.
- Work orders need active/waiting/blocked states and focus links to the responsible
  building/builder.
- Warehouse/resources view should show stock, needs, shortages, and request links; verify
  what MineColonies safely synchronizes before promising a complete client inventory view.
- Keep population, housing, happiness, food, and alerts in this single coherent screen.

## Priority 4: Construction Catalogue and Accurate Placement

- Expand the build drawer into categorized MineColonies buildings with search, favorites,
  style/structure-pack switching, variants, and level selection.
- Building cards should show purpose, footprint, worker capacity, prerequisites, level,
  material summary, and cached Structurize schematic thumbnails.
- Cache previews locally; invalidate on resource/structure-pack changes.
- Add a Roblox-tycoon-style dotted/grid footprint overlay in preview mode:
  green valid ground, red blocked area, clear origin/facing marker, and visible rotation.
- Start snapping with predictable block-grid/footprint behavior. Consider road, shoreline,
  and building alignment snapping only later.
- Show materials in preview mode and show actual missing/delivered requirements when an
  unfinished MineColonies building is selected.
- Do not add opinionated "build this next" recommendations for now.

## Later: Map and Readability Integrations

- Add optional Xaero minimap integration first, then JourneyMap through an adapter layer.
- Display colony, Town Hall, buildings, guards, work orders, alerts, and eventually Create
  infrastructure as markers/overlays.
- Defer world-map integration until the minimap marker layer is useful.
- Add visual-readability settings (night/weather UI contrast, reduced visual clutter,
  accessibility/shader-friendly options). Never alter server time or weather.

## Deferred / Rejected for Now

- Multi-unit selection: revisit when MineColonies supports useful, permission-safe guard
  group commands.
- Planning pause/slow mode: reject; it is unsuitable and misleading for multiplayer.
- Automated next-building recommendations: reject; prefer concrete problem alerts instead.
