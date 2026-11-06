package toolchain.coremod;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

public final class ProbeCoremod implements IFMLLoadingPlugin {
    public String[] getLibraryRequestClass() { return null; }
    public String[] getASMTransformerClass() { return new String[]{"toolchain.coremod.ProbeTransformer"}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) {}
}
