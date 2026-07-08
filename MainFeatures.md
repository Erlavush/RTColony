# RTColony Main Features

## Goal

RTColony turns Minecraft colony management into an RTS-style control layer while keeping final actions server-authoritative through MineColonies and Structurize.

## RTS Mode

- RTS mode auto-enables on world load.
- `F4` toggles RTS mode.
- RTS mode owns camera and input.
- Player movement is blocked while RTS mode is active.
- Vanilla hands, hotbar, crosshair, XP, selected item name, jump meter, and spectator tooltip are hidden.

## RTS Camera

- Normal RTS mode uses an overhead terrain-following camera.
- Mouse wheel zooms.
- Edge panning moves the camera when enabled.
- Edge panning speed is configurable.
- Left mouse drag pans.
- Middle mouse drag rotates.

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
- Middle mouse drag orbits around the building center.
- Vertical orbit is floor-clamped.
- Scroll zooms around the building.
- `Esc` exits back to cursor preview.
- Exiting locked placement returns the camera to the RTS overhead pitch.

## Placement UI

- Locked placement shows a compact right-side Structurize-style tool rail.
- Controls: move X/Z, height, rotate left/right, mirror, cancel, confirm.
- Confirm is the only final placement action.
- Confirm sends `BuildToolPlacementMessage` through MineColonies `SuppliesHandler`.
- RTColony does not directly place MineColonies structures client-side.

## Config

- `O` opens RTColony Config.
- The keybind is rebindable in Minecraft Controls.
- Config file: `config/rtcolony-client.json`.
- Current options:
  - edge panning enabled
  - edge panning speed
  - invert locked placement horizontal orbit
  - invert locked placement vertical orbit

## Main Classes

- `RtsModeState`: RTS enabled state.
- `RtsCameraState`: camera center, zoom, pan, rotation, terrain follow, placement orbit.
- `RtsBuildDrawer`: drawer UI, supply preview, placement states, validation, placement request.
- `RTColonyClientConfig`: player-facing config persistence.
- `RTColonyConfigScreen`: in-game config screen.
- `RTColonyKeyMappings`: rebindable keys.
