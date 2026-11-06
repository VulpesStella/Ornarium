package example;

import cpw.mods.fml.common.Mod;

@Mod(modid = ExampleMod.MOD_ID, name = ExampleMod.MOD_NAME, version = ExampleMod.VERSION)
public final class ExampleMod {
    // Compile-time constants generated from gradle.properties.
    public static final String MOD_ID = BuildConstants.MOD_ID;
    public static final String MOD_NAME = BuildConstants.MOD_NAME;
    public static final String VERSION = BuildConstants.MOD_VERSION;
}
