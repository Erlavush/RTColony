# Reference Study Notes

RTColony keeps cloned third-party projects under `references/` for local study only.
They are intentionally ignored by Git and are not vendored into this repository.

## License

RTColony is licensed as `GPL-3.0-only`.

The current reference projects are also GPL-3.0 projects:

- `references/reignofnether`
- `references/minecolonies`

Because the licenses are compatible, GPL-3.0-derived implementation work is allowed.
When reference code is clearly the strongest approach, port it deliberately into
RTColony instead of treating it as only inspiration. Direct copy/paste still needs a
NeoForge 1.21.1 review because Reign of Nether currently targets older Forge APIs.

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
