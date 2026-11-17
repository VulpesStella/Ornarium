package io.github.vulpesstella.ornarium.api.client;

import net.minecraft.item.ItemStack;

/** Draws an accessory in the third-person player render pass. */
public interface IOrnariumRenderer {
    /** The appearance stack contains the item and metadata, with no gameplay NBT. */
    void render(ItemStack appearance, OrnariumRenderContext context);
}
