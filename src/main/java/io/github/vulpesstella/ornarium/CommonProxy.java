package io.github.vulpesstella.ornarium;

import cpw.mods.fml.common.network.IGuiHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class CommonProxy implements IGuiHandler {
    public void initialize() {}

    public void receivePacket(EntityPlayer player, byte[] payload) {}

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == 0 && x >= 0 && x < SlotRegistry.layout().pageCount() ? new AccessoryContainer(player, x) : null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
