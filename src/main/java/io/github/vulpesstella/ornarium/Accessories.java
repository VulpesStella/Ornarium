package io.github.vulpesstella.ornarium;

import io.github.vulpesstella.ornarium.api.OrnariumTypes;
import io.github.vulpesstella.ornarium.api.IOrnarium;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public final class Accessories implements IInventory, IExtendedEntityProperties {
    private static final String KEY = "Ornarium";
    private static final OrnariumTypes[] TYPES = {
        OrnariumTypes.AMULET, OrnariumTypes.RING, OrnariumTypes.RING, OrnariumTypes.BELT
    };

    private final EntityPlayer owner;
    private final ItemStack[] stacks = new ItemStack[TYPES.length];
    private boolean loaded;

    Accessories(EntityPlayer owner) {
        this.owner = owner;
    }

    public static Accessories of(EntityPlayer player) {
        Accessories inventory = (Accessories) player.getExtendedProperties(KEY);
        if (inventory == null) {
            inventory = new Accessories(player);
            player.registerExtendedProperties(KEY, inventory);
        }
        inventory.load();
        return inventory;
    }

    public static OrnariumTypes getType(int slot) {
        return TYPES[slot];
    }

    private void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        NBTTagList items = persisted().getTagList(KEY);
        for (int index = 0; index < items.tagCount(); index++) {
            NBTBase tag = items.tagAt(index);
            if (tag instanceof NBTTagCompound) {
                NBTTagCompound item = (NBTTagCompound) tag;
                int slot = item.getByte("Slot");
                ItemStack stack = ItemStack.loadItemStackFromNBT(item);
                if (slot >= 0 && slot < stacks.length && stack != null && stack.stackSize > 0) {
                    stacks[slot] = stack;
                }
            }
        }
    }

    private NBTTagCompound persisted() {
        return owner.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    @Override
    public void onInventoryChanged() {
        if (owner.worldObj.isRemote) {
            return;
        }
        NBTTagList items = new NBTTagList();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] != null) {
                NBTTagCompound item = stacks[slot].writeToNBT(new NBTTagCompound());
                item.setByte("Slot", (byte) slot);
                items.appendTag(item);
            }
        }
        NBTTagCompound data = persisted();
        data.setTag(KEY, items);
        owner.getEntityData().setCompoundTag(EntityPlayer.PERSISTED_NBT_TAG, data);
    }

    void tick() {
        boolean worn = false;
        for (int slot = 0; slot < stacks.length; slot++) {
            ItemStack stack = stacks[slot];
            IOrnarium item = asOrnarium(stack);
            if (item != null) {
                item.onWornTick(stack, owner);
                if (stacks[slot] == stack && stack.stackSize <= 0) {
                    setInventorySlotContents(slot, null);
                }
                worn = true;
            }
        }
        if (worn) {
            onInventoryChanged();
        }
    }

    private static IOrnarium asOrnarium(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IOrnarium ? (IOrnarium) stack.getItem() : null;
    }

    boolean canRemove(int slot) {
        IOrnarium item = asOrnarium(stacks[slot]);
        return item == null || item.canUnequip(stacks[slot], owner);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        IOrnarium item = asOrnarium(stack);
        return item != null && item.getOrnariumType(stack) == TYPES[slot] && item.canEquip(stack, owner);
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        ItemStack previous = stacks[slot];
        stacks[slot] = stack;
        if (!owner.worldObj.isRemote && previous != stack) {
            IOrnarium oldItem = asOrnarium(previous);
            IOrnarium newItem = asOrnarium(stack);
            if (oldItem != null) {
                oldItem.onUnequipped(previous, owner);
            }
            if (newItem != null && stacks[slot] == stack) {
                newItem.onEquipped(stack, owner);
            }
        }
        onInventoryChanged();
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        return amount > 0 ? getStackInSlotOnClosing(slot) : null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = stacks[slot];
        setInventorySlotContents(slot, null);
        return stack;
    }

    @Override public ItemStack getStackInSlot(int slot) { return stacks[slot]; }
    @Override public int getSizeInventory() { return stacks.length; }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public String getInvName() { return "ornarium.title"; }
    @Override public boolean isInvNameLocalized() { return false; }
    @Override public boolean isUseableByPlayer(EntityPlayer player) { return player == owner && owner.isEntityAlive(); }
    @Override public void openChest() {}
    @Override public void closeChest() {}
    @Override public void init(Entity entity, World world) {}
    @Override public void loadNBTData(NBTTagCompound nbt) { loaded = false; }
    @Override public void saveNBTData(NBTTagCompound nbt) {}
}
