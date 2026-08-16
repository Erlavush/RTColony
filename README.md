# RTColony

RTColony is a NeoForge 1.21.1 Minecraft Java mod that adds an RTS-style camera,
selection layer, and server-authoritative MineColonies starter-supply placement flow.

Current gameplay includes:

- RTS Perspective, Fixed Angle, True Isometric, and Freecam camera modes cycled with `F4`
- edge/drag panning, zoom, rotation, entity follow, selection outlines, and RTS HUD
- smooth pose transitions from Freecam back to the RTS Perspective camera
- a Sodium-compatible terrain cutaway for obscured selected entities
- MineColonies citizen/building information and native read-only details screens
- Supply Camp and Supply Ship blueprint preview, validation, adjustment, and placement
- an in-game client config screen opened with `O`

The concise control and feature flow lives in `MainFeatures.md`.

## Development environment

RTColony uses Java 21. On this Arch installation, the verified tools are installed
without system-wide packages:

- Temurin 21.0.12+8: `~/.local/opt/jdks/minecraft-java-21`
- IntelliJ IDEA 2026.2.0.1 unified distribution: `~/.local/opt/idea`
- Flite 2.2 for Minecraft narration: `~/.local/opt/flite`
- commands: `~/.local/bin/java21`, `javac21`, `idea`, and `flite`

The unified IntelliJ distribution keeps its core Java/Kotlin feature set free; a paid
license is only needed for the additional Ultimate features.

Load the project JDK and verify it:

```bash
source ./dev-env.sh
```

Build and run all automated tests:

```bash
./gradlew build
```

Open the project in IntelliJ:

```bash
./open-idea.sh
```

Use `~/.local/opt/jdks/minecraft-java-21` as IntelliJ's Project SDK and Gradle JVM.
After a new checkout or `./gradlew clean`, prepare all IntelliJ run configurations with:

```bash
./gradlew prepareClientRun prepareQuickClientRun prepareDataRun prepareServerRun
```

After changing Gradle mod dependencies, refresh IntelliJ's generated module classpath with:

```bash
./gradlew ideaModule
```

The versioned `.run/` configurations are:

- `QuickClient`: opens the `RTCOLONY` singleplayer world
- `Client`: starts the normal development client
- `Server`: starts a headless development server
- `Data`: runs data generation

## Reproducible mod dependencies

Gradle downloads the development mods from their official Maven repositories; do not copy
jars into `run/mods`. The pinned direct versions are:

- MineColonies `1.1.1319-1.21.1-snapshot`
- Structurize `1.0.832-1.21.1-snapshot`
- BlockUI `1.0.199-1.21.1-snapshot` for its public LDTTeam API types
- Jade `15.10.6+neoforge`
- Sodium `mc1.21.1-0.6.13-neoforge`
- Freecam `1.3.0+mc1.21.1` for NeoForge

MineColonies' required BlockUI, Domum Ornamentum, Multi-Piston, Structurize, and LDTTeam
data-generator dependencies are resolved transitively. Optional JEI is intentionally
excluded from the normal development runtime. Freecam is a required client dependency and
must be included separately when assembling a playable modpack; it is not embedded in the
RTColony jar.

## Running and edit loop

Run a normal or quick development client from Gradle with:

```bash
./gradlew runClient
./gradlew runQuickClient
```

Simple method-body changes may HotSwap after IntelliJ Build Project (`Ctrl+F9`) only when
the JVM confirms that classes reloaded. Restart Minecraft after mixin, input, method-shape,
registration, dependency, or mod-metadata changes. Use `F3+T` for resource-only changes.

The client runs include NVIDIA PRIME offload environment variables for this hybrid-GPU
machine. Minecraft's F3 renderer line or `nvidia-smi` can confirm which GPU is active.

GitHub Actions runs the same `./gradlew build` verification on Java 21.

## License

RTColony is licensed under GPL-3.0-only. Copied or adapted reference material is tracked in
`THIRD_PARTY_NOTICES.md`; reference-first rules live in `docs/reference-porting-rules.md`.
