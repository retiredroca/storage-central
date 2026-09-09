package com.retiredroca.storagecentral.registration;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.block.StorageTerminalBlock;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.item.RangeUpgradeItem;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StorageCentral.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StorageCentral.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StorageCentral.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, StorageCentral.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StorageCentral.MODID);

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL_BLOCK =
            BLOCKS.register("storage_terminal", () -> new StorageTerminalBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f)));

    public static final DeferredItem<BlockItem> STORAGE_TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("storage_terminal", STORAGE_TERMINAL_BLOCK);

    public static final DeferredItem<RangeUpgradeItem> RANGE_UPGRADE_TIER1 =
            ITEMS.register("range_upgrade_tier1", () -> new RangeUpgradeItem(new Item.Properties(), 1));
    public static final DeferredItem<RangeUpgradeItem> RANGE_UPGRADE_TIER2 =
            ITEMS.register("range_upgrade_tier2", () -> new RangeUpgradeItem(new Item.Properties(), 2));
    public static final DeferredItem<RangeUpgradeItem> RANGE_UPGRADE_TIER3 =
            ITEMS.register("range_upgrade_tier3", () -> new RangeUpgradeItem(new Item.Properties(), 3));
    public static final DeferredItem<RangeUpgradeItem> RANGE_UPGRADE_TIER4 =
            ITEMS.register("range_upgrade_tier4", () -> new RangeUpgradeItem(new Item.Properties(), 4));
    public static final DeferredItem<RangeUpgradeItem> RANGE_UPGRADE_TIER5 =
            ITEMS.register("range_upgrade_tier5", () -> new RangeUpgradeItem(new Item.Properties(), 5));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL_BE =
            BLOCK_ENTITIES.register("storage_terminal",
                    () -> BlockEntityType.Builder.of(StorageTerminalBlockEntity::new, STORAGE_TERMINAL_BLOCK.get())
                            .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageTerminalMenu>> STORAGE_TERMINAL_MENU =
            MENUS.register("storage_terminal",
                    () -> IMenuTypeExtension.create(StorageTerminalMenu::fromNetwork));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STORAGE_CENTRAL_TAB =
            CREATIVE_MODE_TABS.register("storage_central", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.storage_central"))
                    .icon(() -> new ItemStack(STORAGE_TERMINAL_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(STORAGE_TERMINAL_ITEM.get());
                        output.accept(RANGE_UPGRADE_TIER1.get());
                        output.accept(RANGE_UPGRADE_TIER2.get());
                        output.accept(RANGE_UPGRADE_TIER3.get());
                        output.accept(RANGE_UPGRADE_TIER4.get());
                        output.accept(RANGE_UPGRADE_TIER5.get());
                    })
                    .build());

    private Registration() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
