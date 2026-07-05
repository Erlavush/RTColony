# Third-Party Notices

RTColony is GPL-3.0-only. Some code and assets may be copied or adapted from
compatible open-source Minecraft mods kept locally under `references/`.

## Reign of Nether

- Repository: `https://github.com/SoLegendary/reignofnether`
- Local reference: `references/reignofnether`
- License: GPL-3.0
- Copyright: Reign of Nether contributors

RTColony currently includes the following HUD textures copied from Reign of Nether and
renames their namespace from `reignofnether` to `rtcolony`:

- `src/main/resources/assets/rtcolony/textures/hud/healthbars.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_no_bg.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_left.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_left_small.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_right.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_right_small.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_top.png`
- `src/main/resources/assets/rtcolony/textures/hud/unit_frame_bottom.png`

RTColony also includes the following Reign of Nether item/stat icons under the
`rtcolony` namespace:

- `src/main/resources/assets/rtcolony/textures/icons/items/boots.png`
- `src/main/resources/assets/rtcolony/textures/icons/items/chestplate.png`
- `src/main/resources/assets/rtcolony/textures/icons/items/compass.png`
- `src/main/resources/assets/rtcolony/textures/icons/items/heart.png`
- `src/main/resources/assets/rtcolony/textures/icons/items/sword.png`

The selected entity portrait HUD is also based on Reign of Nether's RTS HUD code,
especially:

- `com.solegendary.reignofnether.hud.PortraitRendererUnit`
- `com.solegendary.reignofnether.hud.PortraitRendererModifiers`
- `com.solegendary.reignofnether.healthbars.HealthBarClientEvents`
- `com.solegendary.reignofnether.util.MyRenderer`

## MineColonies

- Repository: `https://github.com/ldtteam/minecolonies`
- Local reference: `references/minecolonies`
- License: GPL-3.0
- Copyright: MineColonies contributors / LDTTeam

MineColonies is intended as the source of truth for colony concepts, citizens,
buildings, requests, work status, and colony UI/data patterns when RTColony begins
MineColonies integration.

RTColony includes MineColonies builder-hut GUI textures copied under the `rtcolony`
namespace for the RTS build drawer:

- `src/main/resources/assets/rtcolony/textures/gui/minecolonies/builderhut/`

## Structurize

- Local reference: `references/structurize`
- Source jar: `run/mods/structurize-1.0.831-1.21.1-snapshot.jar`
- License declared by jar metadata: GPL 3.0
- Copyright: Structurize contributors / LDTTeam

RTColony includes Structurize build-tool GUI textures copied under the `rtcolony`
namespace for the RTS build drawer:

- `src/main/resources/assets/rtcolony/textures/gui/structurize/buildtool/`

Structurize is the source of truth for build-tool blueprint browsing, preview rendering,
rotation, mirroring, movement controls, structure packs, and placement messages.

## MineFortress

- Repository: `https://github.com/remmintan/minefortress`
- Local reference: `references/minefortress`
- License: MIT
- Copyright: Remmintan

MineFortress is intended as a reference for RTS camera, selection, and vanilla-style
Minecraft GUI behavior. If RTColony copies substantial MineFortress code or assets, keep
the MIT copyright and permission notice with the copied/adapted material.
