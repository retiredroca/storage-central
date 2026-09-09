package com.retiredroca.storagecentral;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.retiredroca.storagecentral.network.Networking;
import com.retiredroca.storagecentral.registration.Registration;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(StorageCentral.MODID)
public class StorageCentral {
    public static final String MODID = "storage_central";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StorageCentral(IEventBus modEventBus) {
        Registration.register(modEventBus);
        Networking.register(modEventBus);
    }
}
