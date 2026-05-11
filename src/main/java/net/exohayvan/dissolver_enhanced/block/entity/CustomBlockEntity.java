package net.exohayvan.dissolver_enhanced.block.entity;

import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class CustomBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, Nameable {
    private LockCode lock;
    @Nullable
    private Component customName;

    protected CustomBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
        this.lock = LockCode.NO_LOCK;
    }

    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.lock = LockCode.fromTag(nbt);
        if (nbt.contains("CustomName", 8)) {
            this.customName = Serializer.fromJson(nbt.getString("CustomName"));
        }

    }

    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        this.lock.addToTag(nbt);
        if (this.customName != null) {
            nbt.putString("CustomName", Serializer.toJson(this.customName));
        }

    }

    public Component getName() {
        return this.customName != null ? this.customName : this.getContainerName();
    }

    public Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    public Component getCustomName() {
        return this.customName;
    }

    protected abstract Component getContainerName();

    public boolean checkUnlocked(Player player) {
        return checkUnlocked(player, this.lock, this.getDisplayName());
    }

    public static boolean checkUnlocked(Player player, LockCode lock, Component containerName) {
        if (!player.isSpectator() && !lock.unlocksWith(player.getMainHandItem())) {
            player.displayClientMessage(Component.translatable("container.isLocked", new Object[]{containerName}), true);
            player.playNotifySound(SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return false;
        } else {
            return true;
        }
    }

    protected abstract NonNullList<ItemStack> getHeldStacks();

    protected abstract void setHeldStacks(NonNullList<ItemStack> inventory);

    public boolean isEmpty() {
        Iterator<ItemStack> var1 = this.getHeldStacks().iterator();

        ItemStack itemStack;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            itemStack = (ItemStack)var1.next();
        } while(itemStack.isEmpty());

        return false;
    }

    public ItemStack getItem(int slot) {
        return (ItemStack)this.getHeldStacks().get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        ItemStack itemStack = ContainerHelper.removeItem(this.getHeldStacks(), slot, amount);
        if (!itemStack.isEmpty()) {
            this.setChanged();
        }

        return itemStack;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.getHeldStacks(), slot);
    }

    public void setItem(int slot, ItemStack stack) {
        this.getHeldStacks().set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    public void clearContent() {
        this.getHeldStacks().clear();
    }

    @Nullable
    public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
        return this.checkUnlocked(playerEntity) ? this.createScreenHandler(i, playerInventory) : null;
    }

    protected abstract AbstractContainerMenu createScreenHandler(int syncId, Inventory playerInventory);

    public void removeComponentsFromTag(CompoundTag nbt) {
        nbt.remove("CustomName");
        nbt.remove("Lock");
        nbt.remove("Items");
    }

    // HOPPER/DROPPER INSERT
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[0];
    }
}
