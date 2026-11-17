package io.github.vulpesstella.ornarium.client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.EnumSet;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import io.github.vulpesstella.ornarium.CommonProxy;
import io.github.vulpesstella.ornarium.Accessories;
import io.github.vulpesstella.ornarium.OrnariumPackets;
import io.github.vulpesstella.ornarium.SlotLayout;
import io.github.vulpesstella.ornarium.SlotRegistry;
import io.github.vulpesstella.ornarium.api.SlotType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.input.Keyboard;

public final class ClientProxy extends CommonProxy {
    private final AccessoryRendering rendering = new AccessoryRendering();

    @Override
    public void initialize() {
        OpenKey binding = new OpenKey();
        KeyBindingRegistry.registerKeyBinding(binding);
        TickRegistry.registerTickHandler(new OpenKeyTick(binding.getKeyBindings()[0]), Side.CLIENT);
        MinecraftForge.EVENT_BUS.register(rendering);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @ForgeSubscribe
    public void onItemTooltip(ItemTooltipEvent event) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof AccessoryScreen && ((AccessoryScreen) screen).isDrawingEquippedTooltip()) {
            return;
        }
        String names = null;
        for (SlotType type : SlotRegistry.matchingTypes(event.itemStack)) {
            String name = I18n.getString(type.nameKey);
            names = names == null ? name : I18n.getStringParams("ornarium.slot.list", names, name);
        }
        if (names != null) {
            event.toolTip.add(EnumChatFormatting.GRAY + I18n.getStringParams("ornarium.slot.compatible", names));
        }
    }

    @Override
    public void receivePacket(EntityPlayer player, byte[] payload) {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload, 1, payload.length - 1));
        try {
            if (payload[0] == OrnariumPackets.LAYOUT) {
                Accessories.of(player).applyClientLayout(SlotLayout.read(input));
            } else if (payload[0] == OrnariumPackets.APPEARANCE) {
                rendering.receive(player, input);
            }
        } catch (IOException exception) {
            FMLLog.warning("Ignoring an invalid Ornarium packet: %s", exception.getMessage());
        }
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == 0 && x >= 0 && x < Accessories.of(player).layout().pageCount()
            ? new AccessoryScreen(player, x) : null;
    }

    private static final class OpenKey extends KeyBindingRegistry.KeyHandler {
        OpenKey() {
            // This constructor registers the binding in Controls without polling the keyboard.
            super(new KeyBinding[] { new KeyBinding("key.ornarium.open", Keyboard.KEY_B) });
        }

        @Override public EnumSet<TickType> ticks() { return EnumSet.noneOf(TickType.class); }
        @Override public String getLabel() { return "Ornarium key binding"; }
        @Override public void keyUp(EnumSet<TickType> types, KeyBinding key, boolean tickEnd) {}
        @Override public void keyDown(EnumSet<TickType> types, KeyBinding key, boolean tickEnd, boolean repeat) {}
    }

    private static final class OpenKeyTick implements ITickHandler {
        private final KeyBinding key;

        OpenKeyTick(KeyBinding key) {
            this.key = key;
        }

        @Override public EnumSet<TickType> ticks() { return EnumSet.of(TickType.CLIENT); }
        @Override public String getLabel() { return "Ornarium key"; }
        @Override public void tickStart(EnumSet<TickType> types, Object... data) {}

        @Override
        public void tickEnd(EnumSet<TickType> types, Object... data) {
            if (!key.isPressed()) {
                return;
            }
            while (key.isPressed()) {
                // Send at most one request per tick.
            }
            Minecraft game = Minecraft.getMinecraft();
            if (game.thePlayer != null && game.currentScreen == null) {
                PacketDispatcher.sendPacketToServer(OrnariumPackets.open());
            }
        }
    }
}
