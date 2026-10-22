package hunternif.mc.impl.atlas.mixin;

import com.stereowalker.unionlib.mod.MinecraftMod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge constructs mods concurrently; UnionLib 12.0.18 uses an unsafe HashMap. */
@Pseudo
@Mixin(targets = "com.stereowalker.unionlib.mod.ModHandler", remap = false)
public abstract class UnionLibModHandlerMixin {
    @Shadow @Final @Mutable
    public static Map<String, MinecraftMod> mods;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void makeModLookupThreadSafe(CallbackInfo callback) {
        mods = new ConcurrentHashMap<>(mods);
    }
}
