package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.screen.MaterializerScreenHandler;
import net.exohayvan.dissolver_enhanced.common.machine.MachineTiming;
import net.exohayvan.dissolver_enhanced.common.machine.MaterializerLogic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

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

    private final PropertyDelegate propertyDelegate;
    private DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private int progress = 0;
    private int storedEmc = 0;

    public MaterializerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MATERIALIZER_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MaterializerBlockEntity.this.progress;
                    case 1 -> MaterializerBlockEntity.this.getConversionTime();
                    case 2 -> MaterializerBlockEntity.this.storedEmc;
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
            public int size() {
                return 5;
            }
        };
    }

    public static void tick(World world, BlockPos pos, BlockState state, MaterializerBlockEntity blockEntity) {
        if (world.isClient) return;

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
            markDirty(world, pos, state);
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

        this.storedEmc = MaterializerLogic.absorbInput(this.storedEmc, inputValue);
        input.decrement(1);
        if (input.isEmpty()) {
            this.stacks.set(EMC_INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private boolean canOutputTarget() {
        ItemStack target = this.stacks.get(TARGET_SLOT);
        int targetValue = getTargetValue();
        if (target.isEmpty() || !MaterializerLogic.canOutput(this.storedEmc, targetValue)) return false;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        if (output.isEmpty()) return true;
        return ItemStack.areItemsAndComponentsEqual(output, target) && output.getCount() < output.getMaxCount();
    }

    private void outputTarget() {
        ItemStack target = this.stacks.get(TARGET_SLOT);
        int targetValue = getTargetValue();
        if (!MaterializerLogic.canOutput(this.storedEmc, targetValue)) return;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            ItemStack materialized = target.copy();
            materialized.setCount(1);
            this.stacks.set(OUTPUT_SLOT, materialized);
        } else if (ItemStack.areItemsAndComponentsEqual(output, target) && output.getCount() < output.getMaxCount()) {
            output.increment(1);
        } else {
            return;
        }

        this.storedEmc = MaterializerLogic.spendForOutput(this.storedEmc, targetValue);
    }

    private int getTargetValue() {
        return stackValue(this.stacks.get(TARGET_SLOT), false);
    }

    private int getInputValue() {
        return stackValue(this.stacks.get(EMC_INPUT_SLOT), true);
    }

    private int getConversionTime() {
        return MachineTiming.ticksForEmc(getInputValue(), CONVERSION_TICKS_PER_EMC);
    }

    private int stackValue(ItemStack stack, boolean allowOrb) {
        if (stack.isEmpty()) return 0;
        if (EMCOrbItem.isEMCOrb(stack)) return allowOrb ? EMCOrbItem.getEMC(stack) : 0;

        String stackKey = EMCKey.fromStack(stack);
        int emc = EMCValues.get(stackKey);
        if (emc <= 0) return 0;

        return MaterializerLogic.getMaterializeValue(stackKey, emc, ItemHelper.getDurabilityPercentage(stack));
    }

    private boolean resetProgress() {
        if (this.progress == 0) return false;

        this.progress = 0;
        return true;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.stacks, registryLookup);
        this.progress = Math.max(0, nbt.getInt("Progress"));
        this.storedEmc = Math.max(0, nbt.getInt("StoredEmc"));
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, this.stacks, registryLookup);
        nbt.putInt("Progress", this.progress);
        nbt.putInt("StoredEmc", this.storedEmc);
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return this.stacks;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.stacks = inventory;
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.dissolver_enhanced.materializer_block");
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new MaterializerScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction direction) {
        if (slot == TARGET_SLOT) return stackValue(stack, false) > 0;
        if (slot == EMC_INPUT_SLOT) return stackValue(stack, true) > 0;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }
}
