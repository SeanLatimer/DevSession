package dev.silentsean.mod.devsession.mixin;

import dev.silentsean.mod.devsession.DevSessionBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.main.Main")
public class MainMixin {

    @ModifyVariable(method = "main", at = @At("HEAD"), argsOnly = true, remap = false)
    private static String[] modifyArgs(String[] args) {
        return DevSessionBootstrap.processArguments(args);
    }

}
