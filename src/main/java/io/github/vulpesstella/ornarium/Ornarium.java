package io.github.vulpesstella.ornarium;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumSet;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
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
import net.minecraftforge.common.Configuration;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;

@Mod(modid = BuildConstants.MOD_ID, name = BuildConstants.MOD_NAME, version = BuildConstants.MOD_VERSION,
    acceptedMinecraftVersions = "[1.6.4]")
@NetworkMod(clientSideRequired = true, serverSideRequired = true, channels = Ornarium.CHANNEL,
    versionBounds = "[" + BuildConstants.MOD_VERSION + "]",
    packetHandler = Ornarium.NetworkHandler.class)
public final class Ornarium {
    public static final String CHANNEL = "Ornarium";

    @Mod.Instance(BuildConstants.MOD_ID)
    public static Ornarium instance;

    @SidedProxy(clientSide = "io.github.vulpesstella.ornarium.client.ClientProxy",
        serverSide = "io.github.vulpesstella.ornarium.CommonProxy")
    public static CommonProxy proxy;

    private Configuration config;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
    }

    @Mod.EventHandler
    public void postInitialize(FMLPostInitializationEvent event) {
        SlotRegistry.freeze(config);
    }

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

    public static final class NetworkHandler implements IPacketHandler {
        @Override
        public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player sender) {
            byte[] payload = packet.data;
            if (payload == null || payload.length == 0 || packet.length != payload.length) {
                return;
            }
            if (!(sender instanceof EntityPlayerMP)) {
                if (sender instanceof EntityPlayer && ((EntityPlayer) sender).worldObj.isRemote) {
                    proxy.receivePacket((EntityPlayer) sender, payload);
                }
                return;
            }
            final EntityPlayerMP player = (EntityPlayerMP) sender;
            if (!player.isEntityAlive() || player.isPlayerSleeping() || player.inventory.getItemStack() != null) {
                return;
            }
            if (payload.length == 1 && payload[0] == OrnariumPackets.OPEN
                && player.openContainer == player.inventoryContainer) {
                player.playerNetServerHandler.sendPacketToPlayer(
                    OrnariumPackets.encode(OrnariumPackets.LAYOUT, new OrnariumPackets.Writer() {
                        @Override public void write(DataOutputStream output) throws IOException {
                            SlotRegistry.layout().write(output);
                        }
                    }));
                player.openGui(instance, 0, player.worldObj, 0, 0, 0);
            } else if (payload.length == 4 && payload[0] == OrnariumPackets.PAGE
                && player.openContainer instanceof AccessoryContainer) {
                AccessoryContainer current = (AccessoryContainer) player.openContainer;
                int page = ((payload[2] & 255) << 8) | (payload[3] & 255);
                if ((payload[1] & 255) == current.windowId && page < current.layout().pageCount()
                    && page != current.pageIndex() && current.isPlayerNotUsingContainer(player)) {
                    player.openGui(instance, 0, player.worldObj, page, 0, 0);
                }
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
            if (!player.worldObj.isRemote) {
                Accessories equipment = Accessories.of(player);
                if (player.isEntityAlive()) {
                    equipment.tick();
                }
                if (player instanceof EntityPlayerMP) {
                    equipment.appearance.update((EntityPlayerMP) player, equipment);
                }
            }
        }
    }
}
