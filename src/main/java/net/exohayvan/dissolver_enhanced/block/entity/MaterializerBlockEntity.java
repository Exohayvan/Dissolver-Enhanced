package net.exohayvan.dissolver_enhanced.block.entity;

import java.math.BigInteger;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.item.EmcCoreItem;
import net.exohayvan.dissolver_enhanced.screen.MaterializerScreenHandler;
import net.exohayvan.dissolver_enhanced.common.machine.MachineTiming;
import net.exohayvan.dissolver_enhanced.common.machine.MaterializerLogic;
import net.exohayvan.dissolver_enhanced.common.values.EmcNumber;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class MaterializerBlockEntity extends CustomBlockEntity {
    public static final int TARGET_SLOT = 0;
    public static final int EMC_INPUT_SLOT = 1;
    public static final int CORE_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    private static final int SIZE = 4;
    private static final int SECONDS_PER_EMC = 1;
    private static final int CONVERSION_TICKS_PER_EMC = MachineTiming.ticksPerEmc(SECONDS_PER_EMC);
    private static final int[] TOP_SLOTS = new int[]{TARGET_SLOT, EMC_INPUT_SLOT};
    private static final int[] BOTTOM_SLOTS = new int[]{OUTPUT_SLOT};
    private static final int[] SIDE_SLOTS = new int[]{EMC_INPUT_SLOT};

    private final ContainerData propertyDelegate;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress = 0;
    private BigInteger storedEmc = BigInteger.ZERO;

    public MaterializerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATERIALIZER_BLOCK_ENTITY.get(), pos, state);
        this.propertyDelegate = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MaterializerBlockEntity.this.progress;
                    case 1 -> MaterializerBlockEntity.this.getConversionTime();
                    case 2 -> EmcNumber.toIntSaturated(MaterializerBlockEntity.this.storedEmc);
                    case 3 -> MaterializerBlockEntity.this.getTargetValue();
                    case 4 -> MaterializerBlockEntity.this.getInputValue();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    MaterializerBlockEntity.this.progress = Math.max(0, value);
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    public static void tick(Level world, BlockPos pos, BlockState state, MaterializerBlockEntity blockEntity) {
        if (world.isClientSide()) return;

        boolean changed = false;
        if (blockEntity.canOutputTarget()) {
            blockEntity.outputTarget();
            blockEntity.resetProgress();
            changed = true;
        } else if (blockEntity.canAbsorbInput()) {
            blockEntity.progress++;
            if (blockEntity.progress >= blockEntity.getConversionTime()) {
                blockEntity.absorbInput();
                blockEntity.resetProgress();
                changed = true;
            }
        } else {
            changed = blockEntity.resetProgress();
        }

        if (blockEntity.progress > 0) {
            changed = true;
        }

        if (changed) {
            setChanged(world, pos, state);
        }
    }

    private boolean canAbsorbInput() {
        if (getTargetValue() <= 0) return false;
        return getInputValue() > 0;
    }

    private void absorbInput() {
        ItemStack input = this.stacks.get(EMC_INPUT_SLOT);
        int inputValue = getInputValue();
        if (inputValue <= 0) return;

        this.storedEmc = MaterializerLogic.absorbInput(this.storedEmc, BigInteger.valueOf(inputValue));
        input.shrink(1);
        if (input.isEmpty()) {
            this.stacks.set(EMC_INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canOutputTarget() {
        ItemStack target = this.stacks.get(TARGET_SLOT);
        int targetValue = getTargetValue();
        if (target.isEmpty() || !MaterializerLogic.canOutput(this.storedEmc, BigInteger.valueOf(targetValue))) return false;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(output, target) && output.getCount() < output.getMaxStackSize();
    }

    private void outputTarget() {
        ItemStack target = this.stacks.get(TARGET_SLOT);
        int targetValue = getTargetValue();
        if (!MaterializerLogic.canOutput(this.storedEmc, BigInteger.valueOf(targetValue))) return;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            ItemStack materialized = target.copy();
            materialized.setCount(1);
            this.stacks.set(OUTPUT_SLOT, materialized);
        } else if (ItemStack.isSameItemSameComponents(output, target) && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
        } else {
            return;
        }

        this.storedEmc = MaterializerLogic.spendForOutput(this.storedEmc, BigInteger.valueOf(targetValue));
    }

    private int getTargetValue() {
        return stackValue(this.stacks.get(TARGET_SLOT), false);
    }

    private int getInputValue() {
        return stackValue(this.stacks.get(EMC_INPUT_SLOT), true);
    }

    private int getConversionTime() {
        return ticksForRate(getInputValue(), getEmcPerSecond());
    }

    private int getEmcPerSecond() {
        return EmcCoreItem.getEmcPerSecond(this.stacks.get(CORE_SLOT));
    }

    private int ticksForRate(int emc, int emcPerSecond) {
        if (emc <= 0) return CONVERSION_TICKS_PER_EMC;

        long ticks = ((long) emc * MachineTiming.TICKS_PER_SECOND + Math.max(1, emcPerSecond) - 1L) / Math.max(1, emcPerSecond);
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, ticks));
    }

    private int stackValue(ItemStack stack, boolean allowOrb) {
        if (stack.isEmpty()) return 0;
        if (EMCOrbItem.isEMCOrb(stack)) return allowOrb ? EMCOrbItem.getEMC(stack) : 0;

        String stackKey = EMCKey.fromStack(stack);
        int emc = EMCValues.get(stackKey);
        if (emc <= 0) return 0;

        return MaterializerLogic.getMaterializeValue(stackKey, emc, ItemHelper.getDurabilityPercentage(stack));
    }

    public BigInteger getStoredEmcForDrop() {
        return EmcNumber.nonNegative(this.storedEmc);
    }

    private boolean resetProgress() {
        if (this.progress == 0) return false;

        this.progress = 0;
        return true;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, this.stacks, registries);
        this.progress = Math.max(0, nbt.getInt("Progress"));
        if (nbt.contains("StoredEmcBig")) {
            this.storedEmc = EmcNumber.parse(nbt.getString("StoredEmcBig"));
        } else {
            this.storedEmc = EmcNumber.of(nbt.getInt("StoredEmc"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        ContainerHelper.saveAllItems(nbt, this.stacks, registries);
        nbt.putInt("Progress", this.progress);
        nbt.putString("StoredEmcBig", EmcNumber.nonNegative(this.storedEmc).toString());
        nbt.putInt("StoredEmc", EmcNumber.toIntSaturated(this.storedEmc));
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
        return Component.translatable("block.dissolver_enhanced.materializer_block");
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected AbstractContainerMenu createScreenHandler(int syncId, Inventory playerInventory) {
        return new MaterializerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (slot == TARGET_SLOT) return stackValue(stack, false) > 0;
        if (slot == EMC_INPUT_SLOT) return stackValue(stack, true) > 0;
        if (slot == CORE_SLOT) return EmcCoreItem.isEmcCore(stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }
}
