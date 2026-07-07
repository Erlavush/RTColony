# RTColony Main Feature

RTColony is a NeoForge 1.21.1 client-side RTS control layer for Minecraft, built to make colony and automation management feel like an RTS while keeping final game actions server-authoritative through existing mod systems.

## Core Mode

- RTS mode is the primary play mode.
- RTS mode auto-enables when a world loads.
- `F4` toggles RTS mode.
- RTS mode detaches the camera from first-person control.
- The player character movement is blocked while RTS mode owns input.
- Vanilla hands, hotbar, crosshair, XP, selected item name, jump meter, and spectator tooltip are hidden in RTS mode.

## RTS Camera

- The camera uses an overhead RTS-style view.
- Mouse wheel zooms the camera.
- Edge panning moves the RTS camera when the cursor reaches screen edges.
- Left mouse drag pans the RTS camera in normal RTS mode.
- Middle mouse drag rotates the RTS camera in normal RTS mode.
- RTS camera height follows terrain in normal RTS mode.

## Build Drawer

- `Ctrl+B` opens the RTColony build drawer while RTS mode is active.
- The drawer uses MineColonies-style paper UI assets.
- The collapsed drawer is a small left-side trapezoid tab.
- The drawer currently contains MineColonies starter supply entries:
  - Supply Camp
  - Supply Ship

## Supply Preview Flow

- Selecting Supply Camp or Supply Ship starts a Structurize blueprint preview.
- The selected blueprint follows the cursor.
- `R` rotates the cursor-following preview.
- `Q/E` or `PageDown/PageUp` adjust preview height.
- `Esc` cancels the cursor-following preview and returns to the drawer.
- Right-click locks the preview into placement-confirm mode.
- Right-click does not confirm final placement.
- `Enter` is not used for placement.

## Locked Placement-Confirm Mode

- The preview stops following the cursor.
- The camera target locks to the center of the preview building.
- Left click is consumed and disabled except for RTColony placement UI buttons.
- Left click must not pan or detach the camera.
- Middle mouse drag orbits the camera around the preview building center.
- Horizontal middle mouse drag rotates around the building.
- Vertical middle mouse drag moves the camera up or down around the building center.
- The orbit camera keeps looking at the building center.
- The orbit camera is floor-clamped so it does not go below terrain.
- Mouse wheel zooms in and out around the locked building.
- `R` rotates the preview.
- `Q/E` or `PageDown/PageUp` adjust preview height.
- `Esc` exits locked placement-confirm mode back to cursor-following preview.
- Exiting locked placement-confirm mode returns the camera to the RTS overhead pitch.

## Placement UI

- Locked placement-confirm mode shows an RTColony placement panel.
- The panel uses Structurize-style button icons.
- The panel provides:
  - move X/Z
  - raise/lower height
  - rotate left
  - rotate right
  - mirror
  - cancel
  - confirm
- Confirm is the only final placement action.
- Confirm sends a Structurize `BuildToolPlacementMessage` through MineColonies `SuppliesHandler`.
- Final placement remains server-authoritative.
- After confirm, the preview clears and the camera returns to the RTS overhead pitch.

## Client Config

- `O` opens the RTColony config screen.
- The `Open RTColony Config` keybind is rebindable in Minecraft Controls.
- The config file is `config/rtcolony-client.json`.
- Current player-facing options:
  - invert locked placement horizontal orbit
  - invert locked placement vertical orbit

## Internal State Shape

- `RtsModeState` owns RTS mode enabled/disabled state.
- `RtsCameraState` owns RTS camera center, yaw, pitch, distance, zoom, pan, rotation, locked placement orbit, and terrain height behavior.
- `RtsBuildDrawer` owns drawer UI, supply selection, preview loading, placement state, validation, placement UI, and final placement request.
- `RTColonyClientConfig` owns player-facing client config persistence.
- `RTColonyConfigScreen` owns the current in-game config UI.
- `RTColonyKeyMappings` owns rebindable key mappings.

## Current Boundaries

- RTColony does not directly place MineColonies supply structures client-side.
- RTColony only sends the existing Structurize/MineColonies placement request.
- RTColony should follow MineColonies and Structurize UI style for placement workflows.
- RTColony should follow RTS reference behavior from Reign of Nether and MineFortress for camera and command ergonomics.
