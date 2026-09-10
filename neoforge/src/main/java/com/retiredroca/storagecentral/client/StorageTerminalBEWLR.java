package com.retiredroca.storagecentral.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;
import com.retiredroca.storagecentral.registration.Registration;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class StorageTerminalBEWLR extends BlockEntityWithoutLevelRenderer {
    private final BlockEntityRenderDispatcher dispatcher;

    public StorageTerminalBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.dispatcher = dispatcher;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        int tier = 0;
        if (stack.has(DataComponents.BLOCK_ENTITY_DATA)) {
            CompoundTag tag = stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
            tier = tag.getInt("tier");
        }

        BlockState defaultState = Registration.STORAGE_TERMINAL_BLOCK.get().defaultBlockState();
        StorageTerminalBlockEntity entity = new StorageTerminalBlockEntity(BlockPos.ZERO, defaultState);

        CompoundTag loadTag = new CompoundTag();
        loadTag.putInt("tier", tier);
        entity.loadWithComponents(loadTag, Minecraft.getInstance().level.registryAccess());

        this.dispatcher.renderItem(entity, poseStack, bufferSource, light, overlay);
    }
}