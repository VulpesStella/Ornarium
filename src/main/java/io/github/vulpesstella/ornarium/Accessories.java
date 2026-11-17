package io.github.vulpesstella.ornarium;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.github.vulpesstella.ornarium.api.IOrnarium;
import io.github.vulpesstella.ornarium.api.OrnariumApi;
import io.github.vulpesstella.ornarium.api.SlotReference;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public final class Accessories implements IInventory, IExtendedEntityProperties {
    private static final String KEY = "Ornarium";
    private static final SlotReference[] LEGACY_SLOTS = {
        OrnariumApi.AMULET, OrnariumApi.FIRST_RING, OrnariumApi.SECOND_RING, OrnariumApi.BELT
    };

    private final EntityPlayer owner;
    final AppearanceSync appearance = new AppearanceSync();
    private final List<NBTTagCompound> pending = new ArrayList<NBTTagCompound>();
    private SlotLayout layout;
    private ItemStack[] stacks;
    private Map<SlotReference, ItemStack> clientAppearance;
    private boolean loaded;

    private Accessories(EntityPlayer owner) {
        this.owner = owner;
        layout = owner.worldObj.isRemote ? SlotLayout.EMPTY : SlotRegistry.layout();
        stacks = new ItemStack[layout.size()];
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

    public SlotLayout layout() { return layout; }

    public void applyClientLayout(SlotLayout updated) {
        if (owner.worldObj.isRemote) {
            layout = updated;
            stacks = new ItemStack[layout.size()];
        }
    }

    public static Map<SlotReference, ItemStack> getClientAppearance(EntityPlayer player) {
        Accessories equipment = (Accessories) player.getExtendedProperties(KEY);
        return equipment == null ? null : equipment.clientAppearance;
    }

    public void setClientAppearance(Map<SlotReference, ItemStack> appearance) {
        clientAppearance = appearance;
    }

    private void load() {
        if (loaded || owner.worldObj.isRemote) {
            return;
        }
        loaded = true;
        Arrays.fill(stacks, null);
        pending.clear();
        NBTTagList items = persisted().getTagList(KEY);
        for (int index = 0; index < items.tagCount(); index++) {
            NBTBase tag = items.tagAt(index);
            if (!(tag instanceof NBTTagCompound)) {
                continue;
            }
            NBTTagCompound item = (NBTTagCompound) tag;
            SlotReference reference = readSlot(item);
            int slot = reference == null ? -1 : layout.indexOf(reference);
            ItemStack stack = ItemStack.loadItemStackFromNBT(item);
            if (!item.getBoolean("Displaced") && slot >= 0 && stacks[slot] == null
                && stack != null && stack.stackSize > 0) {
                stacks[slot] = stack;
            } else {
                pending.add(item);
            }
        }
        onInventoryChanged();
    }

    private static SlotReference readSlot(NBTTagCompound item) {
        if (item.hasKey("Type")) {
            String id = item.getString("Type");
            int index = item.getInteger("Index");
            return SlotRegistry.validId(id) && index >= 0 ? new SlotReference(new ResourceLocation(id), index) : null;
        }
        int index = item.getByte("Slot");
        return item.hasKey("Slot") && index >= 0 && index < LEGACY_SLOTS.length ? LEGACY_SLOTS[index] : null;
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
                SlotReference reference = layout.slot(slot);
                NBTTagCompound item = stacks[slot].writeToNBT(new NBTTagCompound());
                item.setString("Type", reference.type.toString());
                item.setInteger("Index", reference.index);
                items.appendTag(item);
            }
        }
        for (NBTTagCompound item : pending) {
            items.appendTag(item);
        }
        NBTTagCompound data = persisted();
        data.setTag(KEY, items);
        owner.getEntityData().setCompoundTag(EntityPlayer.PERSISTED_NBT_TAG, data);
    }

    void tick() {
        relocateDisplaced();
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

    private void relocateDisplaced() {
        Iterator<NBTTagCompound> items = pending.iterator();
        while (items.hasNext()) {
            NBTTagCompound item = items.next();
            ItemStack stack = ItemStack.loadItemStackFromNBT(item);
            if (stack == null || stack.stackSize <= 0) {
                continue;
            }
            String destination;
            if (putInFirstEmpty(owner.inventory, owner.inventory.mainInventory.length, stack)) {
                destination = "inventory";
            } else if (putInFirstEmpty(owner.getInventoryEnderChest(), owner.getInventoryEnderChest().getSizeInventory(), stack)) {
                destination = "ender_chest";
            } else {
                EntityItem drop = new EntityItem(owner.worldObj, owner.posX, owner.posY + 0.5, owner.posZ, stack);
                drop.delayBeforeCanPickup = 40;
                if (!owner.worldObj.spawnEntityInWorld(drop)) {
                    item.setBoolean("Displaced", true);
                    continue;
                }
                destination = "dropped";
            }
            items.remove();
            onInventoryChanged();
            IOrnarium accessory = asOrnarium(stack);
            if (accessory != null) {
                accessory.onUnequipped(stack, owner);
            }
            if (owner instanceof EntityPlayerMP) {
                ((EntityPlayerMP) owner).sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(
                    "ornarium.relocated." + destination, stack.getDisplayName()));
            }
        }
    }

    private static boolean putInFirstEmpty(IInventory inventory, int size, ItemStack stack) {
        for (int slot = 0; slot < size; slot++) {
            if (inventory.getStackInSlot(slot) == null) {
                inventory.setInventorySlotContents(slot, stack);
                inventory.onInventoryChanged();
                return true;
            }
        }
        return false;
    }

    private static IOrnarium asOrnarium(ItemStack stack) {
        return stack != null && stack.getItem() instanceof IOrnarium ? (IOrnarium) stack.getItem() : null;
    }

    boolean canRemove(int slot) {
        IOrnarium item = asOrnarium(stacks[slot]);
        return item == null || item.canUnequip(stacks[slot], owner);
    }

    public int findFreeSlot(ItemStack stack) {
        for (int slot = 0; slot < stacks.length; slot++) {
            if (stacks[slot] == null && isItemValidForSlot(slot, stack)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null || stack.stackSize < 1) {
            return false;
        }
        IOrnarium item = asOrnarium(stack);
        Collection<ResourceLocation> types = item == null ? null : item.getOrnariumTypes(stack);
        return SlotRegistry.matches(layout.type(slot), stack, types) && (item == null || item.canEquip(stack, owner));
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
        if (stack != null) {
            setInventorySlotContents(slot, null);
        }
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
