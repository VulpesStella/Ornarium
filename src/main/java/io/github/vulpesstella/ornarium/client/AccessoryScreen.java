package io.github.vulpesstella.ornarium.client;

import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.network.PacketDispatcher;
import io.github.vulpesstella.ornarium.AccessoryContainer;
import io.github.vulpesstella.ornarium.OrnariumPackets;
import io.github.vulpesstella.ornarium.SlotLayout;
import io.github.vulpesstella.ornarium.api.SlotReference;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

final class AccessoryScreen extends GuiContainer {
    private static final ResourceLocation INVENTORY = new ResourceLocation("textures/gui/container/inventory.png");

    private final AccessoryContainer container;
    private final SlotLayout.Page page;
    private int tooltipSlot = -1;

    AccessoryScreen(EntityPlayer player, int pageIndex) {
        this(new AccessoryContainer(player, pageIndex));
    }

    private AccessoryScreen(AccessoryContainer container) {
        super(container);
        this.container = container;
        page = container.layout().page(container.pageIndex());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        if (container.layout().pageCount() > 1) {
            GuiButton previous = new PageButton(0, guiLeft + 108, guiTop + 5, "<");
            GuiButton next = new PageButton(1, guiLeft + 154, guiTop + 5, ">");
            previous.enabled = container.pageIndex() > 0;
            next.enabled = container.pageIndex() + 1 < container.layout().pageCount();
            buttonList.add(previous);
            buttonList.add(next);
        }
    }

    private static final class PageButton extends GuiButton {
        private static final int TEXTURE_HEIGHT = 20;

        PageButton(int id, int x, int y, String label) {
            super(id, x, y, 16, 13, label);
        }

        @Override
        public void drawTexturedModalRect(int x, int y, int u, int v, int width, int height) {
            // GuiButton draws the left and right halves separately. Keep the top and bottom borders of each.
            int top = height / 2;
            int bottom = height - top;
            super.drawTexturedModalRect(x, y, u, v, width, top);
            super.drawTexturedModalRect(x, y + top, u, v + TEXTURE_HEIGHT - bottom, width, bottom);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (mc.thePlayer.inventory.getItemStack() == null) {
            int target = container.pageIndex() + (button.id == 0 ? -1 : 1);
            PacketDispatcher.sendPacketToServer(OrnariumPackets.page(container.windowId, target));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(INVENTORY);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawRect(guiLeft + 5, guiTop + 5, guiLeft + 171, guiTop + 82, 0xffc6c6c6);
        for (int slot = 0; slot < page.size(); slot++) {
            Slot position = container.getSlot(slot);
            int x = guiLeft + position.xDisplayPosition;
            int y = guiTop + position.yDisplayPosition;
            drawRect(x - 1, y - 1, x + 16, y + 16, 0xff373737);
            drawRect(x, y, x + 16, y + 16, 0xff8b8b8b);
            if (!position.getHasStack()) {
                ResourceLocation icon = container.layout().type(page.start + slot).icon;
                if (icon == null) {
                    drawRect(x + 6, y + 6, x + 10, y + 10, 0xff696969);
                } else {
                    drawIcon(icon, x, y);
                }
            }
        }
    }

    private void drawIcon(ResourceLocation icon, int x, int y) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(icon);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + 16, 0, 0, 1);
        tessellator.addVertexWithUV(x + 16, y + 16, 0, 1, 1);
        tessellator.addVertexWithUV(x + 16, y, 0, 1, 0);
        tessellator.addVertexWithUV(x, y, 0, 0, 0);
        tessellator.draw();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        boolean paginated = container.layout().pageCount() > 1;
        String group = I18n.getString(page.groupKey);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(group, paginated ? 97 : 160), 8, 8, 0x404040);
        if (paginated) {
            String label = (container.pageIndex() + 1) + "/" + container.layout().pageCount();
            fontRenderer.drawString(label, 139 - fontRenderer.getStringWidth(label) / 2, 8, 0x404040);
        }
        if (page.size() == 0) {
            fontRenderer.drawString(I18n.getString("ornarium.slots.empty"), 8, 28, 0x404040);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (mc.thePlayer.inventory.getItemStack() != null) {
            return;
        }
        int slot = hoveredAccessorySlot(mouseX, mouseY);
        if (slot >= 0 && !container.getSlot(slot).getHasStack()) {
            drawCreativeTabHoveringText(slotLabel(slot), mouseX, mouseY);
        }
    }

    @Override
    protected void drawItemStackTooltip(ItemStack stack, int mouseX, int mouseY) {
        tooltipSlot = hoveredAccessorySlot(mouseX, mouseY);
        try {
            super.drawItemStackTooltip(stack, mouseX, mouseY);
        } finally {
            tooltipSlot = -1;
        }
    }

    boolean isDrawingEquippedTooltip() {
        return tooltipSlot >= 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void drawHoveringText(List lines, int mouseX, int mouseY, FontRenderer font) {
        if (isDrawingEquippedTooltip()) {
            // Vanilla has already assembled and formatted the item's tooltip.
            List<String> equippedLines = new ArrayList<String>(lines);
            equippedLines.add(EnumChatFormatting.GRAY + I18n.getStringParams("ornarium.slot.equipped",
                slotLabel(tooltipSlot)));
            super.drawHoveringText(equippedLines, mouseX, mouseY, font);
        } else {
            super.drawHoveringText(lines, mouseX, mouseY, font);
        }
    }

    private int hoveredAccessorySlot(int mouseX, int mouseY) {
        for (int slot = 0; slot < page.size(); slot++) {
            Slot position = container.getSlot(slot);
            if (isPointInRegion(position.xDisplayPosition, position.yDisplayPosition, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return -1;
    }

    private String slotLabel(int slot) {
        int index = page.start + slot;
        SlotReference reference = container.layout().slot(index);
        return I18n.getStringParams("ornarium.slot.label",
            I18n.getString(container.layout().type(index).nameKey), reference.index + 1);
    }
}
