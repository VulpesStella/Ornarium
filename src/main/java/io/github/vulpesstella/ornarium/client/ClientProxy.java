package io.github.vulpesstella.ornarium.client;

import java.util.EnumSet;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import io.github.vulpesstella.ornarium.CommonProxy;
import io.github.vulpesstella.ornarium.Ornarium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

public final class ClientProxy extends CommonProxy {
    @Override
    public void initialize() {
        OpenKey binding = new OpenKey();
        KeyBindingRegistry.registerKeyBinding(binding);
        TickRegistry.registerTickHandler(new OpenKeyTick(binding.getKeyBindings()[0]), Side.CLIENT);
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return id == 0 ? new AccessoryScreen(player) : null;
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
                PacketDispatcher.sendPacketToServer(new Packet250CustomPayload(Ornarium.CHANNEL, new byte[] { 0 }));
            }
        }
    }
}
