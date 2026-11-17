package io.github.vulpesstella.ornarium.api;

import io.github.vulpesstella.ornarium.Accessories;
import io.github.vulpesstella.ornarium.SlotRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class OrnariumApi {
    public static final SlotReference AMULET = new SlotReference(OrnariumTypes.AMULET, 0);
    public static final SlotReference FIRST_RING = new SlotReference(OrnariumTypes.RING, 0);
    public static final SlotReference SECOND_RING = new SlotReference(OrnariumTypes.RING, 1);
    public static final SlotReference BELT = new SlotReference(OrnariumTypes.BELT, 0);

    private OrnariumApi() {}

    /** Register during Forge pre-initialization or initialization. */
    public static void registerSlotType(SlotType type) {
        SlotRegistry.register(type);
    }

    /** Requests a default capacity. The largest request for each type wins. */
    public static void requestSlots(ResourceLocation type, int count) {
        SlotRegistry.request(type, count);
    }

    /** Returns a copy of the equipped stack, or null when the slot is unavailable or empty. */
    public static ItemStack getEquipped(EntityPlayer player, SlotReference slot) {
        Accessories equipment = Accessories.of(player);
        int index = equipment.layout().indexOf(slot);
        ItemStack stack = index < 0 ? null : equipment.getStackInSlot(index);
        return stack == null ? null : stack.copy();
    }

    /** Moves one item from source into the first free compatible slot on the server. */
    public static boolean tryEquip(EntityPlayer player, ItemStack source) {
        if (player.worldObj.isRemote || source == null || source.stackSize < 1 || !player.isEntityAlive()) {
            return false;
        }
        Accessories equipment = Accessories.of(player);
        int slot = equipment.findFreeSlot(source);
        if (slot < 0) {
            return false;
        }
        equipment.setInventorySlotContents(slot, source.splitStack(1));
        return true;
    }
}
