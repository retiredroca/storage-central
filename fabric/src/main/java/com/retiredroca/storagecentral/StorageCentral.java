package com.retiredroca.storagecentral;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.storagecentral.config.StorageCentralConfig;
import com.retiredroca.storagecentral.network.Networking;
import com.retiredroca.storagecentral.registration.Registration;

import net.fabricmc.api.ModInitializer;

public class StorageCentral implements ModInitializer {
    public static final String MODID = "storage_central";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        StorageCentralConfig.load();
        Registration.register();
        Networking.register();
    }
}