# Reference Study Notes

RTColony keeps cloned third-party projects under `references/` for local study only.
They are intentionally ignored by Git and are not vendored into this repository.

## License

RTColony is licensed as `GPL-3.0-only`.

The current GPL-compatible reference projects are:

- `references/reignofnether`
- `references/minecolonies`

Because the licenses are compatible, GPL-3.0-derived implementation work is allowed.
When reference code is clearly the strongest approach, port it deliberately into
RTColony instead of treating it as only inspiration. Direct copy/paste still needs a
NeoForge 1.21.1 review because Reign of Nether currently targets older Forge APIs.

`references/minefortress` is MIT licensed. Ideas and small adapted utilities are usable,
but substantial copied code needs the MIT notice preserved. Prefer a clean NeoForge
1.21.1 implementation based on its architecture notes rather than directly vendoring
Fabric/Yarn code.

## Reign Of Nether Areas To Study

- `orthoview/OrthoviewClientEvents.java`: camera state, panning, zoom, rotation.
- `mixin/CameraMixin.java`: detached camera behavior.
- `mixin/OrthoViewMixin.java`: orthographic projection hook.
- `guiscreen/TopdownGui.java`: always-visible cursor screen approach.
- `guiscreen/TopdownGuiClientEvents.java`: keeping the RTS GUI active.
- `keybinds/`: RTS and camera keybind organization.
- `hud/`: button and HUD composition patterns.

## MineColonies Areas To Study

- `api/colony/IColonyManager.java`
- `api/colony/IColonyView.java`
- `api/colony/ICitizenDataView.java`
- `api/colony/buildings/views/IBuildingView.java`
- `api/eventbus/events/colony/`
- `core/client/gui/`

For the first RTColony milestone, these are only reference points. MineColonies should
not become a compile dependency until the camera/input prototype is stable.

## MineFortress Areas To Study

- `src/main/java/org/minefortress/mixins/renderer/FortressGameRendererMixin.java`:
  mouse-based raycast direction and selection-manager ticking.
- `src/core/java/net/remmintan/mods/minefortress/core/utils/camera/CameraTools.java`:
  screen-space projection and mouse-to-world view-vector utilities.
- `src/main/java/org/minefortress/mixins/entity/player/FortressClientPlayerEntityMixin.java`:
  overriding player raycast to use the mouse position instead of the crosshair.
- `src/main/java/org/minefortress/mixins/interaction/FortressMouseMixin.java`:
  cursor locking and HUD mouse dispatch.
- `src/main/java/org/minefortress/mixins/interaction/FortressClientInteractionManagerMixin.java`:
  extended reach and input interception in fortress mode.
- `src/main/java/org/minefortress/registries/events/FortressClientEvents.kt`:
  client tick flow, cursor lock policy, and selection key handling.
