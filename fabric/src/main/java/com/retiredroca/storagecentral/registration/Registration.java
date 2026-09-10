package com.retiredroca.storagecentral.registration;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.block.StorageTerminalBlock;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.item.RangeUpgradeItem;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;

public final class Registration {
    public static final StorageTerminalBlock STORAGE_TERMINAL_BLOCK = new StorageTerminalBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f));

    public static final BlockItem STORAGE_TERMINAL_ITEM = new BlockItem(STORAGE_TERMINAL_BLOCK, new Item.Properties());

    public static final RangeUpgradeItem RANGE_UPGRADE_TIER1 =
            new RangeUpgradeItem(new Item.Properties(), 1);
    public static final RangeUpgradeItem RANGE_UPGRADE_TIER2 =
            new RangeUpgradeItem(new Item.Properties(), 2);
    public static final RangeUpgradeItem RANGE_UPGRADE_TIER3 =
            new RangeUpgradeItem(new Item.Properties(), 3);
    public static final RangeUpgradeItem RANGE_UPGRADE_TIER4 =
            new RangeUpgradeItem(new Item.Properties(), 4);
    public static final RangeUpgradeItem RANGE_UPGRADE_TIER5 =
            new RangeUpgradeItem(new Item.Properties(), 5);

    public static final BlockEntityType<StorageTerminalBlockEntity> STORAGE_TERMINAL_BE =
            BlockEntityType.Builder.of(StorageTerminalBlockEntity::new, STORAGE_TERMINAL_BLOCK).build(null);

    public static final ExtendedScreenHandlerType<StorageTerminalMenu, BlockPos> STORAGE_TERMINAL_MENU =
            new ExtendedScreenHandlerType<>(StorageTerminalMenu::fromNetwork, BlockPos.STREAM_CODEC);

    public static final ResourceKey<CreativeModeTab> STORAGE_CENTRAL_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(StorageCentral.MODID, "storage_central"));

    private Registration() {}

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, rl("storage_terminal"), STORAGE_TERMINAL_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, rl("storage_terminal"), STORAGE_TERMINAL_ITEM);
        Registry.register(BuiltInRegistries.ITEM, rl("range_upgrade_tier1"), RANGE_UPGRADE_TIER1);
        Registry.register(BuiltInRegistries.ITEM, rl("range_upgrade_tier2"), RANGE_UPGRADE_TIER2);
        Registry.register(BuiltInRegistries.ITEM, rl("range_upgrade_tier3"), RANGE_UPGRADE_TIER3);
        Registry.register(BuiltInRegistries.ITEM, rl("range_upgrade_tier4"), RANGE_UPGRADE_TIER4);
        Registry.register(BuiltInRegistries.ITEM, rl("range_upgrade_tier5"), RANGE_UPGRADE_TIER5);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, rl("storage_terminal"), STORAGE_TERMINAL_BE);
        Registry.register(BuiltInRegistries.MENU, rl("storage_terminal"), STORAGE_TERMINAL_MENU);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, STORAGE_CENTRAL_TAB_KEY,
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.storage_central"))
                        .icon(() -> new ItemStack(STORAGE_TERMINAL_ITEM))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(STORAGE_CENTRAL_TAB_KEY).register(output -> {
            output.accept(STORAGE_TERMINAL_ITEM);
            output.accept(RANGE_UPGRADE_TIER1);
            output.accept(RANGE_UPGRADE_TIER2);
            output.accept(RANGE_UPGRADE_TIER3);
            output.accept(RANGE_UPGRADE_TIER4);
            output.accept(RANGE_UPGRADE_TIER5);
        });
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(StorageCentral.MODID, path);
    }
}