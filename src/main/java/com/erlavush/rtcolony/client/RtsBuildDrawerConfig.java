package com.erlavush.rtcolony.client;

import com.erlavush.rtcolony.RTColony;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class RtsBuildDrawerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "rtcolony-client-ui.json";
    private static Config current = new Config();
    private static Path configPath;
    private static long lastModifiedMillis = Long.MIN_VALUE;
    private static long lastFailedModifiedMillis = Long.MIN_VALUE;
    private static long nextCheckMillis;

    private RtsBuildDrawerConfig() {
    }

    static Config get(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        if (now >= nextCheckMillis) {
            nextCheckMillis = now + 250L;
            refresh(minecraft);
        }
        return current;
    }

    private static void refresh(Minecraft minecraft) {
        Path path = path(minecraft);
        long modifiedMillis = Long.MIN_VALUE;
        try {
            ensureExists(path);
            modifiedMillis = Files.getLastModifiedTime(path).toMillis();
            if (modifiedMillis == lastModifiedMillis || modifiedMillis == lastFailedModifiedMillis) {
                return;
            }

            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Config loaded = GSON.fromJson(reader, Config.class);
                current = loaded == null ? new Config() : loaded.normalized();
                lastModifiedMillis = modifiedMillis;
                lastFailedModifiedMillis = Long.MIN_VALUE;
                RTColony.LOGGER.info("Reloaded RTColony UI config from {}", path);
            }
        } catch (IOException | JsonSyntaxException exception) {
            lastFailedModifiedMillis = modifiedMillis;
            RTColony.LOGGER.warn("Unable to load RTColony UI config from {}", path, exception);
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

    static final class Config {
        int collapsedTabX = 0;
        int collapsedTabWidth = 26;
        int collapsedTabHeight = 56;
        int collapsedTabTaper = 8;
        int collapsedTabTopMargin = 38;
        int collapsedTabPaperInsetLeft = 2;
        int collapsedTabPaperInsetRight = 2;
        int collapsedTabPaperInsetTop = 1;
        int collapsedTabPaperInsetBottom = 1;
        int collapsedTabPaperOffsetX = 0;
        int collapsedTabPaperOffsetY = 0;
        float collapsedTabPaperSourceX = 18.0F;
        float collapsedTabPaperSourceY = 7.0F;
        float collapsedTabPaperSourceHeight = 116.0F;
        int collapsedTabHighlightInsetLeft = 6;
        int collapsedTabHighlightInsetRight = 8;
        int collapsedArrowOffsetX = -1;
        int collapsedArrowOffsetY = -4;
        float collapsedArrowScale = 1.0F;
        int openArrowOffsetX = 172;
        int openArrowOffsetY = 14;
        float openArrowScale = 1.0F;
        int arrowShadowOffsetX = 1;
        int arrowShadowOffsetY = 1;
        String collapsedArrowText = ">";
        String openArrowText = "<";
        String collapsedTabBorderColor = "0xFF5A3A1F";
        String collapsedTabHighlightColor = "0x33FFFFFF";
        String arrowColor = "0xFF3C2818";
        String arrowShadowColor = "0x80543A22";

        private Config normalized() {
            this.collapsedTabX = Mth.clamp(this.collapsedTabX, -80, 120);
            this.collapsedTabWidth = Mth.clamp(this.collapsedTabWidth, 12, 80);
            this.collapsedTabHeight = Mth.clamp(this.collapsedTabHeight, 20, 180);
            this.collapsedTabTaper = Mth.clamp(this.collapsedTabTaper, 0, this.collapsedTabWidth - 4);
            this.collapsedTabTopMargin = Mth.clamp(this.collapsedTabTopMargin, 0, 240);
            this.collapsedTabPaperInsetLeft = Mth.clamp(this.collapsedTabPaperInsetLeft, 0, this.collapsedTabWidth / 2);
            this.collapsedTabPaperInsetRight = Mth.clamp(this.collapsedTabPaperInsetRight, 0, this.collapsedTabWidth / 2);
            this.collapsedTabPaperInsetTop = Mth.clamp(this.collapsedTabPaperInsetTop, 0, this.collapsedTabHeight / 2);
            this.collapsedTabPaperInsetBottom = Mth.clamp(this.collapsedTabPaperInsetBottom, 0, this.collapsedTabHeight / 2);
            this.collapsedTabPaperOffsetX = Mth.clamp(this.collapsedTabPaperOffsetX, -40, 40);
            this.collapsedTabPaperOffsetY = Mth.clamp(this.collapsedTabPaperOffsetY, -40, 40);
            this.collapsedTabPaperSourceX = Mth.clamp(this.collapsedTabPaperSourceX, 0.0F, 180.0F);
            this.collapsedTabPaperSourceY = Mth.clamp(this.collapsedTabPaperSourceY, 0.0F, 129.0F);
            this.collapsedTabPaperSourceHeight = Mth.clamp(this.collapsedTabPaperSourceHeight, 1.0F, 130.0F - this.collapsedTabPaperSourceY);
            this.collapsedTabHighlightInsetLeft = Mth.clamp(this.collapsedTabHighlightInsetLeft, 0, this.collapsedTabWidth);
            this.collapsedTabHighlightInsetRight = Mth.clamp(this.collapsedTabHighlightInsetRight, 0, this.collapsedTabWidth);
            this.collapsedArrowOffsetX = Mth.clamp(this.collapsedArrowOffsetX, -40, 40);
            this.collapsedArrowOffsetY = Mth.clamp(this.collapsedArrowOffsetY, -40, 40);
            this.collapsedArrowScale = Mth.clamp(this.collapsedArrowScale, 0.5F, 4.0F);
            this.openArrowOffsetX = Mth.clamp(this.openArrowOffsetX, 0, 220);
            this.openArrowOffsetY = Mth.clamp(this.openArrowOffsetY, 0, 80);
            this.openArrowScale = Mth.clamp(this.openArrowScale, 0.5F, 4.0F);
            this.arrowShadowOffsetX = Mth.clamp(this.arrowShadowOffsetX, -8, 8);
            this.arrowShadowOffsetY = Mth.clamp(this.arrowShadowOffsetY, -8, 8);
            if (this.collapsedArrowText == null || this.collapsedArrowText.isBlank()) {
                this.collapsedArrowText = ">";
            }
            if (this.openArrowText == null || this.openArrowText.isBlank()) {
                this.openArrowText = "<";
            }
            return this;
        }

        int borderColor() {
            return parseColor(this.collapsedTabBorderColor, 0xFF5A3A1F);
        }

        int highlightColor() {
            return parseColor(this.collapsedTabHighlightColor, 0x33FFFFFF);
        }

        int arrowColor() {
            return parseColor(this.arrowColor, 0xFF3C2818);
        }

        int arrowShadowColor() {
            return parseColor(this.arrowShadowColor, 0x80543A22);
        }

        private static int parseColor(String value, int fallback) {
            if (value == null) {
                return fallback;
            }

            String normalized = value.trim();
            if (normalized.startsWith("#")) {
                normalized = normalized.substring(1);
            } else if (normalized.startsWith("0x") || normalized.startsWith("0X")) {
                normalized = normalized.substring(2);
            }

            try {
                long parsed = Long.parseUnsignedLong(normalized, 16);
                if (normalized.length() <= 6) {
                    parsed |= 0xFF000000L;
                }
                return (int) parsed;
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }
    }
}
