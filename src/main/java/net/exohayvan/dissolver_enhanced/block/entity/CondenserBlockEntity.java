package net.exohayvan.dissolver_enhanced.block.entity;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.exohayvan.dissolver_enhanced.screen.CondenserScreenHandler;
import net.exohayvan.dissolver_enhanced.common.machine.CondenserLogic;
import net.exohayvan.dissolver_enhanced.common.machine.MachineTiming;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class CondenserBlockEntity extends CustomBlockEntity {
    public static final int INPUT_SLOT = 0;
    public static final int CORE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int SIZE = 3;
    private static final int SECONDS_PER_EMC = 1;
    private static final int CONVERSION_TICKS_PER_EMC = MachineTiming.ticksPerEmc(SECONDS_PER_EMC);
    private static final int[] TOP_SLOTS = new int[]{INPUT_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{INPUT_SLOT};

    private final ContainerData propertyDelegate;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress = 0;

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSER_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> CondenserBlockEntity.this.progress;
                    case 1 -> CondenserBlockEntity.this.getConversionTime();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    CondenserBlockEntity.this.progress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public static void tick(Level world, BlockPos pos, BlockState state, CondenserBlockEntity blockEntity) {
        if (world.isClientSide()) return;

        if (blockEntity.canCondense()) {
            if (EMCOrbItem.isEMCOrb(blockEntity.stacks.get(INPUT_SLOT))) {
                blockEntity.condenseOneItem();
                blockEntity.resetProgress();
                setChanged(world, pos, state);
                return;
            }

            blockEntity.progress++;
            if (blockEntity.progress >= blockEntity.getConversionTime()) {
                blockEntity.condenseOneItem();
                blockEntity.resetProgress();
            }
        } else {
            blockEntity.resetProgress();
        }

        setChanged(world, pos, state);
    }

    private boolean canCondense() {
        ItemStack input = this.stacks.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        BigInteger emc = condenseValueBig(input);
        if (emc.signum() <= 0) return false;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        return output.isEmpty() || (EMCOrbItem.isEMCOrb(output) && output.getCount() == 1);
    }

    private void condenseOneItem() {
        ItemStack input = this.stacks.get(INPUT_SLOT);
        BigInteger emc = condenseValueBig(input);
        if (emc.signum() <= 0) return;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.stacks.set(OUTPUT_SLOT, EMCOrbItem.create(emc));
        } else if (EMCOrbItem.isEMCOrb(output)) {
            EMCOrbItem.setEMC(output, EMCOrbItem.getEmcBig(output).add(emc));
        }

        input.shrink(1);
        if (input.isEmpty()) {
            this.stacks.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private int condenseValue(ItemStack stack) {
        return EmcNumber.toIntSaturated(condenseValueBig(stack));
    }

    private BigInteger condenseValueBig(ItemStack stack) {
        if (EMCOrbItem.isEMCOrb(stack)) {
            return EMCOrbItem.getEmcBig(stack);
        }

        String stackKey = EMCKey.fromStack(stack);
        int emc = EMCValues.get(stackKey);
        if (emc <= 0) return BigInteger.ZERO;

        return BigInteger.valueOf(CondenserLogic.getCondenseValue(stackKey, emc, ItemHelper.getDurabilityPercentage(stack)));
    }

    private int getConversionTime() {
        int emc = condenseValue(this.stacks.get(INPUT_SLOT));
        return ticksForRate(emc, getEmcPerSecond());
    }

    private int getEmcPerSecond() {
        return EmcCoreItem.getEmcPerSecond(this.stacks.get(CORE_SLOT));
    }

    private int ticksForRate(int emc, int emcPerSecond) {
        if (emc <= 0) return CONVERSION_TICKS_PER_EMC;

        long ticks = ((long) emc * MachineTiming.TICKS_PER_SECOND + Math.max(1, emcPerSecond) - 1L) / Math.max(1, emcPerSecond);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, ticks));
    }

    private void resetProgress() {
        this.progress = 0;
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        this.stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.stacks);
        this.progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.stacks);
        output.putInt("Progress", this.progress);
    }

    @Override
    protected NonNullList<ItemStack> getHeldStacks() {
        return this.stacks;
    }

    @Override
    protected void setHeldStacks(NonNullList<ItemStack> inventory) {
        this.stacks = inventory;
    }

    @Override
    protected Component getContainerName() {
        return Component.translatable("block.dissolver_enhanced.condenser_block");
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected AbstractContainerMenu createScreenHandler(int syncId, Inventory playerInventory) {
        return new CondenserScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (slot == CORE_SLOT) return EmcCoreItem.isEmcCore(stack);
        if (slot != INPUT_SLOT) return false;
        return EMCOrbItem.isEMCOrb(stack) ? EMCOrbItem.getEMC(stack) > 0 : EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }
}
