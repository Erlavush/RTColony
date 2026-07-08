# RTColony Handoff

## Purpose

RTColony is a planned NeoForge 1.21.1 Minecraft Java mod for an RTS-style camera and
management interface, initially focused on camera/input/selection and later on
MineColonies and Create integration.

## Current Status

- Project directory: `/home/eru/RTColony`
- Mod name: `RTColony`
- License: `GPL-3.0-only`
- NeoForge scaffold exists once `./gradlew build` passes.
- Java 21 JDK is reused from Prism Launcher and copied to:
  `/home/eru/.local/opt/jdks/minecraft-java-21`
- Convenience commands:
  - `/home/eru/.local/bin/java21`
  - `/home/eru/.local/bin/javac21`
- Current branch: `main`
- Current verification command: `./gradlew build`

## Current Implemented State

- RTS mode auto-enables on world load and can be toggled with `F4`.
- `Ctrl+B` opens the RTColony build drawer while RTS mode is active.
- The build drawer currently supports MineColonies starter supplies:
  - Supply Camp
  - Supply Ship
- Selecting a supply entry starts a Structurize/MineColonies blueprint preview attached
  to the cursor.
- While the preview follows the cursor:
  - right-click locks the preview into placement-confirm mode.
  - `R` rotates the preview.
  - `Q/E` or `PageDown/PageUp` adjust height.
  - `Esc` cancels and returns to the drawer.
- In locked placement-confirm mode:
  - the camera target locks to the preview building center.
  - left-click is consumed/disabled except for clicking RTColony placement UI buttons.
  - middle-mouse drag orbits around the building center.
  - scroll zooms.
  - the placement UI is a compact right-side Structurize-style tool rail for move,
    height, rotate, mirror, cancel, and confirm.
  - confirm sends a Structurize `BuildToolPlacementMessage` through MineColonies'
    `SuppliesHandler`; this is the server-authoritative placement request.
  - `Esc` exits locked placement back to cursor-following preview.
  - exiting locked placement or confirming placement calls `RtsCameraState.returnToRtsView()`,
    so the camera returns to the RTS overhead pitch instead of staying at the low/orbit
    inspection angle.
- RTS mode hides vanilla hands, hotbar, crosshair, XP, selected item name, jump meter,
  and spectator tooltip.
- A first RTColony config screen exists:
  - default keybind `O` opens it.
  - Minecraft Controls can rebind `Open RTColony Config`.
  - config file: `run/config/rtcolony-client.json` in the dev client.
  - current options:
    - edge panning enabled.
    - edge panning speed.
    - invert locked placement horizontal orbit.
    - invert locked placement vertical orbit.
- Drawer visual tuning config still exists separately:
  - `run/config/rtcolony-client-ui.json`
  - ignored under `run/`; do not overwrite the user's local tuning unless explicitly asked.
- `MainFeatures.md` is the concise current feature-flow spec. Keep it short,
  accurate, and free of workflow/process notes.

## Current Workflow Notes

- Camera/input/mixin changes generally require restarting the Minecraft client. Do not
  promise IntelliJ hotswap for mixin changes, new methods, or input behavior changes.
- For simple UI/data-only tweaks, `Ctrl+F9`/Build Project may hotswap only if the JVM
  reports classes were reloaded. If the game says classes are up to date or behavior does
  not change, restart the client.
- The user is testing in IntelliJ with the NeoForge client run config.
- Before committing user-approved gameplay changes, run `./gradlew build`.

## Agent Alignment Rule

- Do not add, expand, or redesign features beyond what the user explicitly requested.
- When a feature request is ambiguous, ask 1-3 short alignment questions before
  implementing. Prefer questions that expose real design choices, expected controls,
  target reference behavior, and acceptance criteria.
- If a multiple-choice user-input tool is available, use it for concise options; include
  the best recommended option first. If that tool is unavailable, ask concise plain-text
  questions.
- If the user explicitly says to choose for them, "just do it", or that they are tired of
  deciding, proceed with a conservative implementation that follows existing project
  patterns and document the assumptions in the final response.
- For brainstorming requests, do not implement immediately. Present the most important
  options, ask the few questions needed to narrow them, and wait for confirmation before
  changing code.
- Keep durable agent-learning notes short and project-specific. Use `AGENTS.md` for
  workflow rules, `MainFeatures.md` for current player-facing feature flow, and avoid
  turning either file into a long process journal.

## Development Rules

- Keep this project separate from `/home/eru/Minecraft-AeroCreate-Server`.
- Do not modify the live AeroCreate server, client instance, or distributed pack while
  working on RTColony unless explicitly requested.
- Treat `references/reignofnether`, `references/minefortress`, and
  `references/minecolonies` as the design source of truth. Do not invent a custom HUD,
  camera, or colony UI style when a compatible reference pattern exists.
- Copy or closely port reference code/assets when licenses allow it, then adapt only what
  is necessary for NeoForge 1.21.1 and RTColony's package/API boundaries.
- Keep copied/adapted third-party material attributed in `THIRD_PARTY_NOTICES.md`.
- Start with a small NeoForge 1.21.1 client-side prototype before adding MineColonies or
  Create dependencies.
- Prefer Gradle wrapper tasks from the project once the NeoForge scaffold exists.
- Use IntelliJ IDEA Community for Java editing/debugging.
- Use the Prism-derived Java 21 JDK for Gradle and IDE project SDK.
- Keep third-party reference clones under ignored `references/`.

## Commit and Push Rule

- After each completed requested change, commit and push immediately before starting
  another unrelated change.
- If the user says a feature is done, accepts it, or moves on to another feature,
  treat the current completed feature as accepted and commit/push it before continuing.
- Do not commit or push when the user explicitly says to skip, pause, avoid, or not do
  the commit/push.
- Do not leave completed work only in the working tree unless the user explicitly asks
  not to commit or push.
- When committing a feature that changes the mod's current behavior, also update
  `MainFeatures.md` with the short, accurate feature flow. Remove outdated or unnecessary
  details from that file.
- Commit messages must be short, real, and specific to the completed change. Do not use
  a generic message when the change has a clear feature, fix, or docs scope.
- Use this command sequence:

```bash
git status --short
git add -A
git commit -m "<type>: <short accurate change summary>"
git push
```

## Planned Milestones

1. Empty NeoForge 1.21.1 project scaffold.
2. F4 keybind toggles RTS mode.
3. Client input ownership while RTS mode is active.
4. Basic overhead/isometric camera state.
5. Camera panning, zoom, and rotation.
6. Selection raycast and highlight rendering.
7. Minimal RTS HUD.
8. Read-only MineColonies integration.
9. Read-only Create integration.
10. Controlled server-authoritative actions.
