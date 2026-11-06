package io.github.vulpesstella.ornarium.api;

import io.github.vulpesstella.ornarium.Accessories;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class OrnariumApi {
    public static final int AMULET = 0;
    public static final int FIRST_RING = 1;
    public static final int SECOND_RING = 2;
    public static final int BELT = 3;

    private OrnariumApi() {}

    public static OrnariumTypes getSlotType(int slot) {
        return Accessories.getType(slot);
    }

    /** Returns a copy of the equipped stack. Read authoritative equipment on the server. */
    public static ItemStack getEquipped(EntityPlayer player, int slot) {
        ItemStack stack = Accessories.of(player).getStackInSlot(slot);
        return stack == null ? null : stack.copy();
    }

    /** Moves one item from source into the first free compatible slot on the server. */
    public static boolean tryEquip(EntityPlayer player, ItemStack source) {
        if (player.worldObj.isRemote || source == null || source.stackSize < 1 || !player.isEntityAlive()) {
            return false;
        }
        Accessories equipment = Accessories.of(player);
        for (int slot = 0; slot < equipment.getSizeInventory(); slot++) {
            if (equipment.getStackInSlot(slot) == null && equipment.isItemValidForSlot(slot, source)) {
                equipment.setInventorySlotContents(slot, source.splitStack(1));
                return true;
            }
        }
        return false;
    }
}
