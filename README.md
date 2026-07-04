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

Run the development client:

```bash
./gradlew runClient
```

Run the development client and directly open the `RTCOLONY` singleplayer world:

```bash
./gradlew runQuickClient
```

The `runClient` configuration is set to use NVIDIA PRIME offload on hybrid
Intel/NVIDIA laptops. To confirm it, open the F3 screen in Minecraft and check that the
renderer mentions NVIDIA, or run `nvidia-smi` in another terminal and look for the Java
process while Minecraft is open.

## License

RTColony is licensed under GPL-3.0-only.
