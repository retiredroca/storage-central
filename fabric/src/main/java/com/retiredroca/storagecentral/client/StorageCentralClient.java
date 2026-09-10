package com.retiredroca.storagecentral.client;

import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.network.Networking;
import com.retiredroca.storagecentral.registration.Registration;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class StorageCentralClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Registration.STORAGE_TERMINAL_MENU, StorageTerminalScreen::new);
        Networking.registerClient();
    }
}