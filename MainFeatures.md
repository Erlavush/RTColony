# RTColony Main Features

## Goal

RTColony turns Minecraft colony management into an RTS-style control layer while keeping final actions server-authoritative through MineColonies and Structurize.

## RTS Mode

- RTS mode auto-enables on world load.
- `F4` cycles between RTS Perspective and True Isometric. From vanilla view, it returns to
  the last RTS camera mode.
- `F5` exits RTS mode to vanilla Minecraft view. Once outside RTS mode, vanilla `F5`
  perspective cycling works normally.
- RTS mode owns camera and input.
- Player movement is blocked while RTS mode is active.
- Vanilla hands, hotbar, crosshair, XP, selected item name, jump meter, and spectator tooltip are hidden.

## RTS Camera

- RTS Perspective uses an overhead terrain-following camera. Smooth terrain-height changes
  are enabled by default; disabling that option snaps camera height to the new terrain level
  immediately, with no vertical transition animation.
- True Isometric uses an orthographic, fixed-angle RTS projection with no perspective depth
  or terrain-height bobbing.
- Mouse wheel zooms through a closer range in both RTS camera modes.
- Edge panning moves the camera when enabled.
- Edge panning speed is configurable with a slider.
- Left mouse drag pans.
- Right mouse drag rotates in RTS Perspective.
- True Isometric right mouse drag rotates in discrete 90-degree quarter turns.

## Selection and Focus

- Right-click selects a block, entity, citizen, or loaded MineColonies building.
- The original compact entity portrait and stats container remains at the bottom-left.
- Entity and citizen selection adds a compact `Follow` action above that container. While
  following, the camera tracks the entity; manual pan, rotate, zoom, `Esc`, or starting a
  build preview exits follow cleanly.
- MineColonies citizens and buildings add a `Details` action that opens their native,
  read-only MineColonies screen.
- Loaded MineColonies buildings are selectable across their full schematic bounds. The
  complete building area is outlined and drives the building information panel.
- When a selected or followed entity becomes genuinely hidden behind terrain,
  RTColony enables a target-aware clean cutaway around that entity.
- The cutaway is disabled when no entity is selected/followed or when the entity
  is already visible.
- The cutaway follows the entity's projected screen bounds and opens/closes by
  resizing a clean ellipse. It does not use full-area dotted or screen-door
  transparency.
- Multi-point line-of-sight checks and short hysteresis windows prevent
  flickering near wall and foliage edges.
- Sodium and Iris terrain compatibility remains supported through the existing
  shader-patching path.
- RTColony does not add citizen or building control actions until a supported,
  permission-safe, server-authoritative MineColonies path is defined.

## Build Drawer

- `Ctrl+B` opens the MineColonies-style build drawer.
- Current entries are Supply Camp and Supply Ship.
- Selecting an entry starts a Structurize blueprint preview that follows the cursor.

## Cursor Preview

- Right-click locks the preview into placement-confirm mode.
- `R` rotates.
- `Q/E` or `PageDown/PageUp` adjust height.
- `Esc` cancels and returns to the drawer.
- `Enter` is not used.

## Locked Placement

- The preview stops following the cursor.
- The camera locks to the preview building center.
- Left click is consumed except for RTColony placement UI buttons.
- RTS Perspective right mouse drag orbits around the building center.
- True Isometric stays orthographic during locked placement; right mouse drag rotates in
  discrete 90-degree quarter turns around the preview.
- Vertical orbit is floor-clamped.
- Scroll zooms around the building.
- `W/A/S/D` move the preview relative to the current camera direction.
- `Q/R` rotate left/right.
- `F` mirrors.
- `Enter` confirms placement.
- `Esc` cancels and returns to the open build drawer.
- Canceling or confirming placement returns the camera to the RTS overhead pitch.

## Placement UI

- Locked placement shows a compact right-side Structurize-style tool rail.
- Controls: move X/Z, height, rotate left/right, mirror, cancel, confirm.
- Confirm is the only final placement action.
- Blueprint previews show a dotted footprint grid. The grid is green for valid placement,
  red for invalid/missing-item placement, and marks the blueprint origin in blue.
- Confirm sends `BuildToolPlacementMessage` through MineColonies `SuppliesHandler`.
- RTColony does not directly place MineColonies structures client-side.

## Config

- `O` opens RTColony Config.
- The keybind is rebindable in Minecraft Controls.
- Config file: `config/rtcolony-client.json`.
- Current options:
  - edge panning enabled
  - edge panning speed
  - smooth terrain-height changes
  - invert locked placement horizontal orbit
  - invert locked placement vertical orbit

## Main Classes

- `RtsModeState`: RTS enabled state.
- `RtsCameraState`: camera center, zoom, pan, rotation, terrain follow, placement orbit.
- `RtsTargetingState`: cursor targeting, selection, building bounds, and entity follow.
- `RtsFocusLensShader`: target-aware terrain cutaway lens for Sodium and Iris terrain shaders.
- `RtsBuildDrawer`: drawer UI, supply preview, placement states, validation, placement request.
- `RTColonyClientConfig`: player-facing config persistence.
- `RTColonyConfigScreen`: in-game config screen.
- `RTColonyKeyMappings`: rebindable keys.
