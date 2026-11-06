package io.github.vulpesstella.ornarium;

import java.util.EnumSet;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;

@Mod(modid = BuildConstants.MOD_ID, name = BuildConstants.MOD_NAME, version = BuildConstants.MOD_VERSION,
    acceptedMinecraftVersions = "[1.6.4]")
@NetworkMod(clientSideRequired = true, serverSideRequired = true, channels = Ornarium.CHANNEL,
    packetHandler = Ornarium.OpenRequest.class)
public final class Ornarium {
    public static final String CHANNEL = "Ornarium";

    @Mod.Instance(BuildConstants.MOD_ID)
    public static Ornarium instance;

    @SidedProxy(clientSide = "io.github.vulpesstella.ornarium.client.ClientProxy",
        serverSide = "io.github.vulpesstella.ornarium.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void initialize(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        NetworkRegistry.instance().registerGuiHandler(this, proxy);
        TickRegistry.registerTickHandler(new WearTick(), Side.SERVER);
        MinecraftForge.EVENT_BUS.register(this);
        proxy.initialize();
    }

    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        EntityPlayer player = event.entityPlayer;
        if (player.worldObj.isRemote || player.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory")) {
            return;
        }
        Accessories equipment = Accessories.of(player);
        for (int slot = 0; slot < equipment.getSizeInventory(); slot++) {
            ItemStack stack = equipment.getStackInSlotOnClosing(slot);
            if (stack != null) {
                EntityItem drop = new EntityItem(player.worldObj, player.posX, player.posY, player.posZ, stack);
                drop.delayBeforeCanPickup = 40;
                event.drops.add(drop);
            }
        }
    }

    public static final class OpenRequest implements IPacketHandler {
        @Override
        public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player sender) {
            if (packet.length != 1 || packet.data == null || packet.data.length != 1 || packet.data[0] != 0
                || !(sender instanceof EntityPlayerMP)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (player.isEntityAlive() && !player.isPlayerSleeping() && player.openContainer == player.inventoryContainer
                && player.inventory.getItemStack() == null) {
                player.openGui(instance, 0, player.worldObj, 0, 0, 0);
            }
        }
    }

    private static final class WearTick implements ITickHandler {
        @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.PLAYER); }
        @Override public String getLabel() { return "Ornarium equipment"; }
        @Override public void tickStart(EnumSet<TickType> types, Object... data) {}

        @Override
        public void tickEnd(EnumSet<TickType> types, Object... data) {
            EntityPlayer player = (EntityPlayer) data[0];
            if (!player.worldObj.isRemote && player.isEntityAlive()) {
                Accessories.of(player).tick();
            }
        }
    }
}
