package com.retiredroca.storagecentral.registration;

import com.retiredroca.storagecentral.StorageCentral;
import com.retiredroca.storagecentral.block.StorageTerminalBlock;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.menu.StorageTerminalMenu;
import com.retiredroca.storagecentral.network.Networking;
import com.retiredroca.storagecentral.recipe.TerminalUpgradeRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.component.DataComponents;

public final class Registration {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StorageCentral.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StorageCentral.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StorageCentral.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, StorageCentral.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StorageCentral.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, StorageCentral.MODID);

    public static final DeferredBlock<StorageTerminalBlock> STORAGE_TERMINAL_BLOCK =
            BLOCKS.register("storage_terminal", () -> new StorageTerminalBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f).noOcclusion()));

    public static final DeferredItem<BlockItem> STORAGE_TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("storage_terminal", STORAGE_TERMINAL_BLOCK);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> TERMINAL_UPGRADE_RECIPE =
            RECIPE_SERIALIZERS.register("terminal_upgrade", () -> TerminalUpgradeRecipe.Serializer.INSTANCE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL_BE =
            BLOCK_ENTITIES.register("storage_terminal",
                    () -> BlockEntityType.Builder.of(StorageTerminalBlockEntity::new, STORAGE_TERMINAL_BLOCK.get())
                            .build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<StorageTerminalMenu>> STORAGE_TERMINAL_MENU =
            MENUS.register("storage_terminal",
                    () -> IMenuTypeExtension.create(StorageTerminalMenu::fromNetwork));

    private static final String[] TIER_NAMES = {
        "Copper", "Iron", "Gold", "Emerald", "Diamond", "Netherite"
    };

    public static ItemStack terminalWithTier(int tier) {
        ItemStack stack = new ItemStack(STORAGE_TERMINAL_ITEM.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("tier", tier);
        BlockEntity.addEntityType(tag, getTerminalBEType());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(TIER_NAMES[tier] + " Storage Terminal"));
        return stack;
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STORAGE_CENTRAL_TAB =
            CREATIVE_MODE_TABS.register("storage_central", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.storage_central"))
                    .icon(() -> terminalWithTier(0))
                    .displayItems((params, output) -> {
                        if (!Networking.isServerModded()) {
                            return;
                        }
                        for (int i = 0; i < TIER_NAMES.length; i++) {
                            output.accept(terminalWithTier(i));
                        }
                    })
                    .build());

    public static Item getTerminalItem() {
        return STORAGE_TERMINAL_BLOCK.get().asItem();
    }

    public static BlockEntityType<StorageTerminalBlockEntity> getTerminalBEType() {
        return STORAGE_TERMINAL_BE.get();
    }

    private Registration() {}

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
