package com.retiredroca.storagecentral.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.retiredroca.storagecentral.StorageCentral;

import net.fabricmc.loader.api.FabricLoader;

public final class StorageCentralConfig {
    public static final int MAX_SUPPORTED_TIER = 5;

    private static int maxTier = MAX_SUPPORTED_TIER;

    private StorageCentralConfig() {}

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("storage_central.json");
        if (!Files.exists(configPath)) {
            writeDefault(configPath);
            return;
        }
        try {
            JsonElement element = JsonParser.parseString(Files.readString(configPath));
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("maxTier")) {
                    int value = obj.get("maxTier").getAsInt();
                    maxTier = Math.max(0, Math.min(MAX_SUPPORTED_TIER, value));
                }
            }
        } catch (Exception e) {
            StorageCentral.LOGGER.error("Failed to load storage_central config, using defaults", e);
        }
    }

    private static void writeDefault(Path configPath) {
        JsonObject obj = new JsonObject();
        obj.addProperty("maxTier", MAX_SUPPORTED_TIER);
        try {
            Files.createDirectories(configPath.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(configPath, gson.toJson(obj));
        } catch (Exception e) {
            StorageCentral.LOGGER.error("Failed to write default storage_central config", e);
        }
    }

    public static int getMaxTier() {
        return maxTier;
    }
}