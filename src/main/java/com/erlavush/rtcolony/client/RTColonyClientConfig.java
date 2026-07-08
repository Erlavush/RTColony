package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RTColonyClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "rtcolony-client.json";
    private static Config current = new Config();
    private static Path configPath;
    private static long lastModifiedMillis = Long.MIN_VALUE;
    private static long nextCheckMillis;

    private RTColonyClientConfig() {
    }

    public static Config get(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        if (now >= nextCheckMillis) {
            nextCheckMillis = now + 250L;
            refresh(minecraft);
        }
        return current;
    }

    public static void setInvertLockedPlacementOrbitHorizontal(Minecraft minecraft, boolean value) {
        Config config = get(minecraft);
        config.invertLockedPlacementOrbitHorizontal = value;
        save(minecraft);
    }

    public static void setInvertLockedPlacementOrbitVertical(Minecraft minecraft, boolean value) {
        Config config = get(minecraft);
        config.invertLockedPlacementOrbitVertical = value;
        save(minecraft);
    }

    public static void setEdgePanningEnabled(Minecraft minecraft, boolean value) {
        Config config = get(minecraft);
        config.edgePanningEnabled = value;
        save(minecraft);
    }

    public static void setEdgePanningSensitivity(Minecraft minecraft, float value) {
        Config config = get(minecraft);
        config.edgePanningSensitivity = value;
        save(minecraft);
    }

    private static void refresh(Minecraft minecraft) {
        Path path = path(minecraft);
        try {
            ensureExists(path);
            long modifiedMillis = Files.getLastModifiedTime(path).toMillis();
            if (modifiedMillis == lastModifiedMillis) {
                return;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Config loaded = GSON.fromJson(reader, Config.class);
                current = loaded == null ? new Config() : loaded.normalized();
                lastModifiedMillis = modifiedMillis;
                RTColony.LOGGER.info("Reloaded RTColony client config from {}", path);
            }
        } catch (IOException | JsonSyntaxException exception) {
            RTColony.LOGGER.warn("Unable to load RTColony client config from {}", path, exception);
        }
    }

    private static void save(Minecraft minecraft) {
        Path path = path(minecraft);
        try {
            Files.createDirectories(path.getParent());
            current = current.normalized();
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
            lastModifiedMillis = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            RTColony.LOGGER.warn("Unable to save RTColony client config to {}", path, exception);
        }
    }

    private static Path path(Minecraft minecraft) {
        if (configPath == null) {
            configPath = minecraft.gameDirectory.toPath().resolve("config").resolve(CONFIG_FILE_NAME);
        }
        return configPath;
    }

    private static void ensureExists(Path path) throws IOException {
        if (Files.exists(path)) {
            return;
        }

        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(new Config(), writer);
        }
    }

    public static final class Config {
        private boolean invertLockedPlacementOrbitHorizontal;
        private boolean invertLockedPlacementOrbitVertical;
        private Boolean edgePanningEnabled = true;
        private Float edgePanningSensitivity = 1.0F;

        private Config normalized() {
            if (this.edgePanningEnabled == null) {
                this.edgePanningEnabled = true;
            }
            if (this.edgePanningSensitivity == null || this.edgePanningSensitivity <= 0.0F) {
                this.edgePanningSensitivity = 1.0F;
            }
            this.edgePanningSensitivity = Math.max(0.25F, Math.min(3.0F, this.edgePanningSensitivity));
            return this;
        }

        public boolean invertLockedPlacementOrbitHorizontal() {
            return this.invertLockedPlacementOrbitHorizontal;
        }

        public boolean invertLockedPlacementOrbitVertical() {
            return this.invertLockedPlacementOrbitVertical;
        }

        public boolean edgePanningEnabled() {
            return Boolean.TRUE.equals(this.edgePanningEnabled);
        }

        public float edgePanningSensitivity() {
            return this.edgePanningSensitivity == null ? 1.0F : this.edgePanningSensitivity;
        }
    }
}
