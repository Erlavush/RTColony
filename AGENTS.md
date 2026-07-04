# RTColony Handoff

## Purpose

RTColony is a planned NeoForge 1.21.1 Minecraft Java mod for an RTS-style camera and
management interface, initially focused on camera/input/selection and later on
MineColonies and Create integration.

## Current Status

- Project directory: `/home/eru/RTColony`
- Mod name: `RTColony`
- No mod source has been scaffolded yet.
- Java 21 JDK is reused from Prism Launcher and copied to:
  `/home/eru/.local/opt/jdks/minecraft-java-21`
- Convenience commands:
  - `/home/eru/.local/bin/java21`
  - `/home/eru/.local/bin/javac21`

## Development Rules

- Keep this project separate from `/home/eru/Minecraft-AeroCreate-Server`.
- Do not modify the live AeroCreate server, client instance, or distributed pack while
  working on RTColony unless explicitly requested.
- Start with a small NeoForge 1.21.1 client-side prototype before adding MineColonies or
  Create dependencies.
- Prefer Gradle wrapper tasks from the project once the NeoForge scaffold exists.
- Use IntelliJ IDEA Community for Java editing/debugging.
- Use the Prism-derived Java 21 JDK for Gradle and IDE project SDK.

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

