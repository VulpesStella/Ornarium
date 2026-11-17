package io.github.vulpesstella.ornarium;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import io.github.vulpesstella.ornarium.api.SlotReference;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

final class AppearanceSync {
    private static final int REFRESH_TICKS = 20;

    private int[] lastAppearance;

    void update(final EntityPlayerMP player, final Accessories equipment) {
        final int[] appearance = new int[equipment.getSizeInventory() * 2];
        int count = 0;
        if (player.isEntityAlive()) {
            for (int slot = 0; slot < equipment.getSizeInventory(); slot++) {
                ItemStack stack = equipment.getStackInSlot(slot);
                if (stack != null && stack.stackSize > 0) {
                    appearance[slot * 2] = stack.itemID;
                    appearance[slot * 2 + 1] = stack.getItemDamage();
                    count++;
                }
            }
        }
        final int visible = count;
        boolean changed = lastAppearance == null ? visible > 0 : !Arrays.equals(lastAppearance, appearance);
        if (changed || (visible > 0 && player.ticksExisted % REFRESH_TICKS == 0)) {
            player.getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(player,
                OrnariumPackets.encode(OrnariumPackets.APPEARANCE, new OrnariumPackets.Writer() {
                    @Override public void write(DataOutputStream output) throws IOException {
                        output.writeInt(player.entityId);
                        output.writeShort(visible);
                        for (int slot = 0; slot < equipment.getSizeInventory(); slot++) {
                            int id = appearance[slot * 2];
                            if (id > 0) {
                                SlotReference reference = equipment.layout().slot(slot);
                                output.writeUTF(reference.type.toString());
                                output.writeShort(reference.index);
                                output.writeInt(id);
                                output.writeInt(appearance[slot * 2 + 1]);
                            }
                        }
                    }
                }));
        }
        lastAppearance = visible > 0 ? appearance : null;
    }
}
