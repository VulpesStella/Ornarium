package example.mixin;

import example.MappingProbe;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Shadow protected WorldInfo worldInfo;

    @Inject(method = "getWorldTime", at = @At("RETURN"))
    private void test$time(CallbackInfoReturnable<Long> result) {
        if (worldInfo == null || worldInfo.getWorldTime() != result.getReturnValue().longValue())
            throw new AssertionError("Mapped World.worldInfo field disagrees with return value");
        MappingProbe.calls++;
        MappingProbe.fieldChecks++;
    }
}
