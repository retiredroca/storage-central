package com.retiredroca.storagecentral.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.retiredroca.storagecentral.block.StorageTerminalBlock;
import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class StorageTerminalRenderer implements BlockEntityRenderer<StorageTerminalBlockEntity> {
    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.fromNamespaceAndPath("storage_central", "textures/entity/chest/copper.png"),
            ResourceLocation.fromNamespaceAndPath("storage_central", "textures/entity/chest/iron.png"),
            ResourceLocation.fromNamespaceAndPath("storage_central", "textures/entity/chest/gold.png"),
            ResourceLocation.fromNamespaceAndPath("storage_central", "textures/entity/chest/emerald.png"),
            ResourceLocation.fromNamespaceAndPath("storage_central", "textures/entity/chest/diamond.png"),
            ResourceLocation.fromNamespaceAndPath("storage_central", "textures/entity/chest/netherite.png") };

    private final ModelPart root;
    private final ModelPart lid;
    private final ModelPart lock;
    private final ModelPart bottom;

    public StorageTerminalRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart baked = context.bakeLayer(ModelLayers.CHEST);
        this.root = baked;
        this.lid = baked.getChild("lid");
        this.lock = baked.getChild("lock");
        this.bottom = baked.getChild("bottom");
    }

    private ResourceLocation textureFor(int tier) {
        if (tier < 0) {
            return TEXTURES[0];
        }
        if (tier >= TEXTURES.length) {
            return TEXTURES[TEXTURES.length - 1];
        }
        return TEXTURES[tier];
    }

    @Override
    public void render(StorageTerminalBlockEntity entity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay) {
        float openness = entity.getOpenNess(partialTick);
        float eased = 1.0F - (float) Math.pow(1.0F - (double) openness, 3.0);

        Direction facing = entity.getBlockState().getValue(StorageTerminalBlock.FACING);
        float rotation = facing.get2DDataValue() * 90.0F;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, -0.5, -0.5);

        this.lid.xRot = -eased * 1.5707964F;
        this.lock.xRot = -eased * 1.5707964F;

        VertexConsumer consumer = bufferSource
                .getBuffer(RenderType.entityCutout(textureFor(entity.getTier())));
        this.root.render(poseStack, consumer, light, overlay);
        poseStack.popPose();
    }
}