package net.exohayvan.dissolver_enhanced.block.entity;

import net.exohayvan.dissolver_enhanced.data.EMCValues;
import net.exohayvan.dissolver_enhanced.helpers.EMCKey;
import net.exohayvan.dissolver_enhanced.helpers.ItemHelper;
import net.exohayvan.dissolver_enhanced.item.EMCOrbItem;
import net.exohayvan.dissolver_enhanced.screen.CondenserScreenHandler;
import net.exohayvan.dissolver_enhanced.common.machine.CondenserLogic;
import net.exohayvan.dissolver_enhanced.common.machine.MachineTiming;
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

    private final PropertyDelegate propertyDelegate;
    private DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private int progress = 0;

    public CondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSER_BLOCK_ENTITY, pos, state);
        this.propertyDelegate = new PropertyDelegate() {
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
            public int size() {
                return 2;
            }
        };
    }

    public static void tick(World world, BlockPos pos, BlockState state, CondenserBlockEntity blockEntity) {
        if (world.isClient) return;

        if (blockEntity.canCondense()) {
            if (EMCOrbItem.isEMCOrb(blockEntity.stacks.get(INPUT_SLOT))) {
                blockEntity.condenseOneItem();
                blockEntity.resetProgress();
                markDirty(world, pos, state);
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

        markDirty(world, pos, state);
    }

    private boolean canCondense() {
        ItemStack input = this.stacks.get(INPUT_SLOT);
        if (input.isEmpty()) return false;

        int emc = condenseValue(input);
        if (emc <= 0) return false;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        return output.isEmpty() || (EMCOrbItem.isEMCOrb(output) && output.getCount() == 1);
    }

    private void condenseOneItem() {
        ItemStack input = this.stacks.get(INPUT_SLOT);
        int emc = condenseValue(input);
        if (emc <= 0) return;

        ItemStack output = this.stacks.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            this.stacks.set(OUTPUT_SLOT, EMCOrbItem.create(emc));
        } else if (EMCOrbItem.isEMCOrb(output)) {
            EMCOrbItem.setEMC(output, CondenserLogic.safeAdd(EMCOrbItem.getEMC(output), emc));
        }

        input.decrement(1);
        if (input.isEmpty()) {
            this.stacks.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private int condenseValue(ItemStack stack) {
        if (EMCOrbItem.isEMCOrb(stack)) {
            return EMCOrbItem.getEMC(stack);
        }

        String stackKey = EMCKey.fromStack(stack);
        int emc = EMCValues.get(stackKey);
        if (emc <= 0) return 0;

        return CondenserLogic.getCondenseValue(stackKey, emc, ItemHelper.getDurabilityPercentage(stack));
    }

    private int getConversionTime() {
        int emc = condenseValue(this.stacks.get(INPUT_SLOT));
        return MachineTiming.ticksForEmc(emc, CONVERSION_TICKS_PER_EMC);
    }

    private void resetProgress() {
        this.progress = 0;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        this.stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, this.stacks, registryLookup);
        this.progress = nbt.getInt("Progress");
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, this.stacks, registryLookup);
        nbt.putInt("Progress", this.progress);
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
        return Text.translatable("block.dissolver_enhanced.condenser_block");
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new CondenserScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction direction) {
        if (slot != INPUT_SLOT) return false;
        return EMCOrbItem.isEMCOrb(stack) ? EMCOrbItem.getEMC(stack) > 0 : EMCValues.get(EMCKey.fromStack(stack)) > 0;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT_SLOT;
    }
}
