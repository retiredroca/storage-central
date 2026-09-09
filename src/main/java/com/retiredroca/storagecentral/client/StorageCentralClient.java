package com.retiredroca.storagecentral.client;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.registration.Registration;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = StorageCentral.MODID, dist = Dist.CLIENT)
public class StorageCentralClient {
    public StorageCentralClient(IEventBus modEventBus) {
        modEventBus.addListener(StorageCentralClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.STORAGE_TERMINAL_MENU.get(), StorageTerminalScreen::new);
    }
}
