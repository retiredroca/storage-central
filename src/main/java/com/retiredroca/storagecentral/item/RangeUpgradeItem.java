package com.retiredroca.storagecentral.item;

import java.util.List;

import com.retiredroca.storagecentral.blockentity.StorageTerminalBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RangeUpgradeItem extends Item {
    private final int tier;

    public RangeUpgradeItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof StorageTerminalBlockEntity terminal) {
            Player player = context.getPlayer();
            boolean applied = terminal.tryApplyUpgrade(tier, player);
            if (applied && player != null && !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.storage_central.range_upgrade.tier",
                tier + 1).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.storage_central.range_upgrade.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
