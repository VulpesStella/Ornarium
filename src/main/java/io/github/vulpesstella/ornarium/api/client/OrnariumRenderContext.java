package io.github.vulpesstella.ornarium.api.client;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

public final class OrnariumRenderContext {
    public enum Anchor {
        HEAD, BODY, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG
    }

    private final EntityPlayer player;
    private final ResourceLocation slotType;
    private final int slotIndex;
    private final float partialTick;
    private final ModelBiped model;

    public OrnariumRenderContext(EntityPlayer player, ResourceLocation slotType, int slotIndex,
        float partialTick, ModelBiped model) {
        this.player = player;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.partialTick = partialTick;
        this.model = model;
    }

    public EntityPlayer getPlayer() { return player; }
    public ResourceLocation getSlotType() { return slotType; }
    public int getSlotIndex() { return slotIndex; }
    public float getPartialTick() { return partialTick; }

    /** Applies the animated model part's transform to the current model-view matrix. */
    public void attachTo(Anchor anchor) {
        ModelRenderer part;
        switch (anchor) {
            case HEAD: part = model.bipedHead; break;
            case BODY: part = model.bipedBody; break;
            case LEFT_ARM: part = model.bipedLeftArm; break;
            case RIGHT_ARM: part = model.bipedRightArm; break;
            case LEFT_LEG: part = model.bipedLeftLeg; break;
            case RIGHT_LEG: part = model.bipedRightLeg; break;
            default: throw new AssertionError(anchor);
        }
        part.postRender(1.0F / 16.0F);
    }
}
