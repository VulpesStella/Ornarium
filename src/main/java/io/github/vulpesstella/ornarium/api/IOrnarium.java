package io.github.vulpesstella.ornarium.api;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

/** Implement on an Item to make it wearable. Callbacks run on the logical server. */
public interface IOrnarium {
    OrnariumTypes getOrnariumType(ItemStack stack);
    boolean canEquip(ItemStack stack, EntityLivingBase wearer);
    boolean canUnequip(ItemStack stack, EntityLivingBase wearer);
    void onEquipped(ItemStack stack, EntityLivingBase wearer);
    void onUnequipped(ItemStack stack, EntityLivingBase wearer);
    void onWornTick(ItemStack stack, EntityLivingBase wearer);
}
