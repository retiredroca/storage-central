package com.retiredroca.storagecentral.client;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.network.Networking;
import com.retiredroca.storagecentral.registration.Registration;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = StorageCentral.MODID, dist = Dist.CLIENT)
public class StorageCentralClient {
    public StorageCentralClient(IEventBus modEventBus) {
        modEventBus.addListener(StorageCentralClient::registerScreens);
        modEventBus.addListener(StorageCentralClient::registerRenderers);
        modEventBus.addListener(StorageCentralClient::registerItemExtensions);
        NeoForge.EVENT_BUS.addListener(StorageCentralClient::onLoggingOut);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        Networking.setServerModded(false);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.STORAGE_TERMINAL_MENU.get(), StorageTerminalScreen::new);
    }

    private static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.STORAGE_TERMINAL_BE.get(), StorageTerminalRenderer::new);
    }

    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft mc = Minecraft.getInstance();
                return new StorageTerminalBEWLR(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
            }
        }, Registration.STORAGE_TERMINAL_ITEM.get());
    }
}
