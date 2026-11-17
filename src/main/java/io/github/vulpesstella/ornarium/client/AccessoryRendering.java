package io.github.vulpesstella.ornarium.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.relauncher.ReflectionHelper;
import io.github.vulpesstella.ornarium.Accessories;
import io.github.vulpesstella.ornarium.SlotLayout;
import io.github.vulpesstella.ornarium.api.SlotReference;
import io.github.vulpesstella.ornarium.api.client.IOrnariumRenderer;
import io.github.vulpesstella.ornarium.api.client.OrnariumRenderContext;
import io.github.vulpesstella.ornarium.api.client.OrnariumRenderers;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.opengl.GL11;

public final class AccessoryRendering {
    private static final Field PLAYER_MODEL = ReflectionHelper.findField(RenderPlayer.class,
        "modelBipedMain", "field_77109_a");

    void receive(EntityPlayer localPlayer, DataInputStream input) throws IOException {
        int entityId = input.readInt();
        Entity entity = localPlayer.entityId == entityId ? localPlayer : localPlayer.worldObj.getEntityByID(entityId);
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        int count = input.readUnsignedShort();
        if (count > SlotLayout.MAX_SLOTS) {
            throw new IOException("Too many Ornarium appearance entries");
        }
        Map<SlotReference, ItemStack> stacks = new LinkedHashMap<SlotReference, ItemStack>();
        for (int slot = 0; slot < count; slot++) {
            String type = input.readUTF();
            int index = input.readUnsignedShort();
            int id = input.readInt();
            int damage = input.readInt();
            if (type.length() > 64 || index >= SlotLayout.MAX_SLOTS) {
                throw new IOException("Invalid Ornarium appearance slot");
            }
            if (id > 0 && id < Item.itemsList.length && Item.itemsList[id] != null) {
                stacks.put(new SlotReference(new ResourceLocation(type), index), new ItemStack(id, 1, damage));
            }
        }
        if (input.available() != 0) {
            throw new IOException("Trailing Ornarium appearance data");
        }
        Accessories.of((EntityPlayer) entity).setClientAppearance(stacks.isEmpty() ? null : stacks);
    }

    @ForgeSubscribe
    public void renderAccessories(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        Map<SlotReference, ItemStack> stacks = Accessories.getClientAppearance(player);
        if (stacks == null || player.isInvisible() || !player.isEntityAlive()) {
            return;
        }
        for (Map.Entry<SlotReference, ItemStack> entry : stacks.entrySet()) {
            ItemStack stack = entry.getValue();
            IOrnariumRenderer renderer = OrnariumRenderers.get(stack.getItem());
            if (renderer != null) {
                SlotReference reference = entry.getKey();
                OrnariumRenderContext context = new OrnariumRenderContext(player, reference.type, reference.index,
                    event.partialRenderTick, getModel(event.renderer));
                draw(renderer, stack, context);
            }
        }
    }

    private static ModelBiped getModel(RenderPlayer renderer) {
        try {
            return (ModelBiped) PLAYER_MODEL.get(renderer);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access the player model", exception);
        }
    }

    private static void draw(IOrnariumRenderer renderer, ItemStack stack, OrnariumRenderContext context) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL11.glColor4f(1, 1, 1, 1);
            renderer.render(stack.copy(), context);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }
}
