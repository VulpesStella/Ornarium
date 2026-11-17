package io.github.vulpesstella.ornarium.api;

import java.util.Collection;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Implement on an Item to provide slot choices and server-side equipment callbacks. */
public interface IOrnarium {
    Collection<ResourceLocation> getOrnariumTypes(ItemStack stack);
    boolean canEquip(ItemStack stack, EntityLivingBase wearer);
    boolean canUnequip(ItemStack stack, EntityLivingBase wearer);
    void onEquipped(ItemStack stack, EntityLivingBase wearer);
    void onUnequipped(ItemStack stack, EntityLivingBase wearer);
    void onWornTick(ItemStack stack, EntityLivingBase wearer);
}
