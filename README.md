# RTColony

RTColony is a planned NeoForge 1.21.1 Minecraft Java mod for an RTS-style camera and
management interface.

The first development target is a small client-side prototype:

- F4 keybind to toggle RTS mode
- overhead/isometric camera state
- camera panning, zoom, and rotation
- click/raycast selection
- simple selection highlight and HUD

MineColonies and Create integration are planned after the camera/input prototype is
stable.

## Development

This project uses a Java 21 JDK copied from the local Prism Launcher runtime:

```bash
source ./dev-env.sh
```

Open the project in IntelliJ IDEA Community:

```bash
./open-idea.sh
```

The NeoForge scaffold is present. Use `./gradlew build` for the first verification pass.

## License

RTColony is licensed under GPL-3.0-only.
