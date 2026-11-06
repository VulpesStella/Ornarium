package example;

import example.probe.BranchProbe;
import example.probe.ProbeEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

/** Assertions run inside the real Forge classloader, not against mocked APIs. */
public final class MappingProbe {
    public static int calls;
    public static int fieldChecks;
    private static int events;

    public static void verify(World world) {
        int before = calls;
        int fieldsBefore = fieldChecks;
        world.getWorldTime();
        require(calls == before + 1, "exactly one mapped method injection");
        require(fieldChecks == fieldsBefore + 1, "mapped shadow field access");
        BranchProbe probe = new BranchProbe(3);
        require(probe.exercise(0) == 8, "branch/constructor/finally/anonymous class");
        require(probe.exercise(-1) == 10, "exception handler/finally");
        require(BranchProbe.transformed(), "coremod bytecode transformer");
        try {
            BranchProbe.class.getDeclaredMethod("clientOnly");
            throw new AssertionError("SideTransformer did not remove client-only method");
        } catch (NoSuchMethodException expected) {
            // The server must see the side-transformed class.
        }
        require(ProbeEvent.class.getDeclaredMethods().length > 0, "Forge event methods generated");
        EventHandler handler = new EventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        try {
            MinecraftForge.EVENT_BUS.post(new ProbeEvent());
            require(events == 1, "Forge event dispatch");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(handler);
        }
        System.out.println("[RETROSPECTUM-TEST] mapped method executed");
        System.out.println("[RETROSPECTUM-TEST] scanner/side/coremod/event/frames checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static final class EventHandler {
        @ForgeSubscribe
        public void event(ProbeEvent event) { events++; }
    }
}
