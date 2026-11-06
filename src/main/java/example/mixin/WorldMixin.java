package example.mixin;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldMixin {
    // Constructor signatures differ between client and server; names are never obfuscated.
    @Inject(method = "<init>*", at = @At("RETURN"), remap = false)
    private void example$worldCreated(CallbackInfo ci) {
        System.out.println("[EXAMPLE-MOD] World created");
    }
}
