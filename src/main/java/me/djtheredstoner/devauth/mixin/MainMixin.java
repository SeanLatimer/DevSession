package me.djtheredstoner.devauth.mixin;

import me.djtheredstoner.devauth.DevAuthBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.client.main.Main")
public class MainMixin {

    @ModifyVariable(method = "main", at = @At("HEAD"), argsOnly = true, remap = false)
    private static String[] modifyArgs(String[] args) {
        return DevAuthBootstrap.processArguments(args);
    }

}
