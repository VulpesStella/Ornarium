package io.github.vulpesstella.ornarium.client;

import java.util.Locale;

import io.github.vulpesstella.ornarium.AccessoryContainer;
import io.github.vulpesstella.ornarium.api.OrnariumApi;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

final class AccessoryScreen extends GuiContainer {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");

    AccessoryScreen(EntityPlayer player) {
        super(new AccessoryContainer(player));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(INVENTORY);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawRect(guiLeft + 5, guiTop + 5, guiLeft + 171, guiTop + 82, 0xffc6c6c6);
        for (int slot = OrnariumApi.AMULET; slot <= OrnariumApi.BELT; slot++) {
            Slot position = inventorySlots.getSlot(slot);
            int x = guiLeft + position.xDisplayPosition;
            int y = guiTop + position.yDisplayPosition;
            drawRect(x - 1, y - 1, x + 16, y + 16, 0xff373737);
            drawRect(x, y, x + 16, y + 16, 0xff8b8b8b);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.getString("ornarium.title"), 105, 8, 0x404040);
        for (int slot = OrnariumApi.AMULET; slot <= OrnariumApi.BELT; slot++) {
            String type = OrnariumApi.getSlotType(slot).name().toLowerCase(Locale.ROOT);
            fontRenderer.drawString(I18n.getString("ornarium.slot." + type), 30,
                inventorySlots.getSlot(slot).yDisplayPosition + 4, 0x404040);
        }
    }
}
