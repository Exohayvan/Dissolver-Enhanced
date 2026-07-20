package net.exohayvan.dissolver_enhanced.block;

import net.exohayvan.dissolver_enhanced.analytics.ModAnalytics;
import net.exohayvan.dissolver_enhanced.helpers.ActionResultCompat;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

abstract class ScreenOpeningBlock extends BlockWithEntity {
    private final String analyticsId;

    protected ScreenOpeningBlock(Settings settings, String analyticsId) {
        super(settings);
        this.analyticsId = analyticsId;
    }

    @Override
    protected final BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected final ActionResult onUse(
        BlockState state,
        World world,
        BlockPos pos,
        PlayerEntity player,
        BlockHitResult hit
    ) {
        if (world.isClient()) return ActionResultCompat.success(true);

        ModAnalytics.captureBlockUse(analyticsId);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof NamedScreenHandlerFactory screenFactory) {
            player.openHandledScreen(screenFactory);
        }
        return ActionResultCompat.consume();
    }
}
