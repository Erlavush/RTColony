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

The development runtime resolves MineColonies from its official LDTTeam Maven repository
at `com.ldtteam:minecolonies:1.1.1319-1.21.1-snapshot`.

RTColony includes MineColonies builder-hut GUI textures copied under the `rtcolony`
namespace for the RTS build drawer:

- `src/main/resources/assets/rtcolony/textures/gui/minecolonies/builderhut/`

## Structurize

- Repository: `https://github.com/ldtteam/Structurize`
- Local reference: `references/structurize`
- Development artifact: `com.ldtteam:structurize:1.0.832-1.21.1-snapshot`
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

## Freecam

- Repository: `https://github.com/MinecraftFreecam/Freecam`
- Modrinth project: `https://modrinth.com/mod/freecam`
- Development artifact: `maven.modrinth:XeEZ3fK2:ROfcbxxe`
- Version: `1.3.0+mc1.21.1` for NeoForge 1.21.1
- License: MIT
- Copyright: Freecam contributors, including hashalite and Matt Sturgeon

RTColony uses Freecam as an external client dependency and calls its public camera API for
the fourth `F4` camera mode. The Freecam jar is not copied into RTColony's source or output
jar. If it is bundled later, its MIT license and copyright notice must accompany it.

## Dungeons Perspective

- Repository: `https://github.com/cleannrooster/dungeons-perspective`
- Local reference: `references/dungeons-perspective`
- License: MIT, as clarified by the repository owner in issue #26
- Copyright: cleannrooster and Dungeons Perspective contributors

RTColony's Sodium terrain cutaway is adapted from Dungeons Perspective's block-renderer
mixin, camera-to-entity cone/cylinder culling, connected-air `FloodCuller`, and affected
section rebuild approach. RTColony changes the focus to its selected/followed entity and
also rebuilds when the camera, target, animation, or approved blocker set changes.
