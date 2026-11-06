package example.probe;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.concurrent.Callable;

public final class BranchProbe {
    private final int value;

    public BranchProbe(int input) {
        if (input < 0) throw new IllegalArgumentException("negative input");
        value = input;
    }

    public int exercise(int input) {
        int result;
        try {
            if (input < 0) throw new IllegalArgumentException("negative path");
            switch (input) {
                case 0: result = value + 1; break;
                case 1: result = value + 2; break;
                default: result = 0;
            }
            result += new Callable<Integer>() {
                public Integer call() { return Integer.valueOf(2); }
            }.call().intValue();
        } catch (IllegalArgumentException expected) {
            result = 8;
        } finally {
            // Keep an exception/finally control-flow shape for verifier coverage.
            System.getProperty("java.version");
        }
        return result + 2;
    }

    public static boolean transformed() { return false; }

    @SideOnly(Side.CLIENT)
    public void clientOnly() { throw new AssertionError("server must strip this method"); }
}
