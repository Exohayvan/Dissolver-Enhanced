package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.config.ModConfig;
import net.exohayvan.dissolver_enhanced.helpers.EMCHelper;
import net.exohayvan.dissolver_enhanced.screen.DissolverScreenHandler;
import net.exohayvan.dissolver_enhanced.screen.ModScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DissolverBlockEntity extends CustomBlockEntity {
    private NonNullList<ItemStack> inputStacks;

    public DissolverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISSOLVER_BLOCK_ENTITY.get(), pos, state);
        // redstone input slot
        this.inputStacks = NonNullList.withSize(1, ItemStack.EMPTY); 
    }

    public AbstractContainerMenu createScreenHandler(int syncId, Inventory playerInventory) {
        DissolverScreenHandler handler = new DissolverScreenHandler(syncId, playerInventory);
        ModScreenHandlers.activeHandlers.put(playerInventory.player.getUUID(), handler);
        return handler;
    }

    protected void setHeldStacks(NonNullList<ItemStack> itemList) {
        if (ModConfig.PRIVATE_EMC) return;
        
        EMCHelper.addItem(itemList.get(0), this.level);
        this.inputStacks = itemList;
    }

    public NonNullList<ItemStack> getHeldStacks() {
        return this.inputStacks;
    }

    protected Component getContainerName() {
        return Component.translatable("block.dissolver_enhanced.dissolver_block");
    }

    public int getContainerSize() {
        // redstone input slot size
        return 1;
    }

    public ItemStack getRenderStack() {
        return this.getItem(0);
    }

    // HOPPER/DROPPER INSERT (WIP not working)
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        // DissolverEnhanced.LOGGER.info("INSERTING........");
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

	// public int getComparatorOutput() {
	// 	int i = 0;
    // WIP get 15 * (items learned / all items)
	// 	return i;
	// }
}
