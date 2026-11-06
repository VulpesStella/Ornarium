package io.github.vulpesstella.ornarium;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public final class AccessoryContainer extends Container {
    private final Accessories equipment;
    private final int inventoryStart;
    private final int hotbarStart;

    public AccessoryContainer(EntityPlayer player) {
        equipment = Accessories.of(player);
        inventoryStart = equipment.getSizeInventory();
        hotbarStart = inventoryStart + 27;
        for (int slot = 0; slot < inventoryStart; slot++) {
            addSlotToContainer(new AccessorySlot(slot));
        }
        for (int slot = 0; slot < 36; slot++) {
            int index = (slot + 9) % 36;
            int y = slot < 27 ? 84 + 18 * (slot / 9) : 142;
            addSlotToContainer(new Slot(player.inventory, index, 8 + 18 * (slot % 9), y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return equipment.isUseableByPlayer(player);
    }

    @Override
    public ItemStack slotClick(int index, int button, int mode, EntityPlayer player) {
        // Vanilla's number-key shortcut can move a whole hotbar stack into a one-item slot.
        if (index >= 0 && index < inventoryStart && mode == 2 && button >= 0 && button < 9) {
            ItemStack hotbar = player.inventory.getStackInSlot(button);
            if (hotbar != null && hotbar.stackSize > 1) {
                return null;
            }
        }
        return super.slotClick(index, button, mode, player);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = getSlot(index);
        if (!slot.getHasStack() || !slot.canTakeStack(player)) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < inventoryStart) {
            ItemStack moving = stack.copy();
            if (!mergeItemStack(moving, inventoryStart, inventorySlots.size(), true)) {
                return null;
            }
            slot.putStack(null);
        } else {
            boolean equipped = false;
            for (int target = 0; target < inventoryStart; target++) {
                Slot destination = getSlot(target);
                if (!destination.getHasStack() && destination.isItemValid(stack)) {
                    destination.putStack(stack.splitStack(1));
                    equipped = true;
                    break;
                }
            }
            if (!equipped && !(index < hotbarStart
                ? mergeItemStack(stack, hotbarStart, inventorySlots.size(), false)
                : mergeItemStack(stack, inventoryStart, hotbarStart, false))) {
                return null;
            }
            if (stack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
        }
        slot.onPickupFromSlot(player, original);
        return original;
    }

    private final class AccessorySlot extends Slot {
        AccessorySlot(int slot) {
            super(equipment, slot, 8, 12 + 16 * slot);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return equipment.isItemValidForSlot(getSlotIndex(), stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return equipment.canRemove(getSlotIndex());
        }
    }
}
