package io.github.vulpesstella.ornarium.debug;

import io.github.vulpesstella.ornarium.api.OrnariumTypes;
import io.github.vulpesstella.ornarium.api.client.IOrnariumRenderer;
import io.github.vulpesstella.ornarium.api.client.OrnariumRenderContext;
import io.github.vulpesstella.ornarium.api.client.OrnariumRenderers;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class DebugClientProxy extends DebugAccessories.CommonProxy {
    @Override
    public void registerRenderers(Item amulet) {
        OrnariumRenderers.register(amulet, new AmuletRenderer());
    }

    private static final class AmuletRenderer extends ModelBase implements IOrnariumRenderer {
        private final ModelRenderer chain = new ModelRenderer(this, 0, 0);
        private final ModelRenderer pendant = new ModelRenderer(this, 0, 0);

        AmuletRenderer() {
            chain.addBox(-3, 0, -3, 1, 4, 1);
            chain.addBox(2, 0, -3, 1, 4, 1);
            chain.addBox(-3, 3, -3, 6, 1, 1);
            pendant.addBox(-1, 4, -3.5F, 2, 3, 1);
        }

        @Override
        public void render(ItemStack appearance, OrnariumRenderContext context) {
            if (!OrnariumTypes.AMULET.equals(context.getSlotType())) {
                return;
            }
            context.attachTo(OrnariumRenderContext.Anchor.BODY);
            if (context.getPlayer().getCurrentArmor(2) != null) {
                // Clear the extra thickness of the vanilla chestplate model.
                GL11.glTranslatef(0.0F, 0.0F, -1.25F / 16.0F);
            }
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1.0F, 0.77F, 0.24F);
            chain.render(1.0F / 16.0F);
            GL11.glColor3f(0.20F, 0.85F, 0.43F);
            pendant.render(1.0F / 16.0F);
        }
    }
}
