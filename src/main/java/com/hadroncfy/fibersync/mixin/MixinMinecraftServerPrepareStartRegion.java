package com.hadroncfy.fibersync.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This mixin completely skips prepareStartRegion() to avoid issues in 1.21.11
 * The spawn region will be prepared lazily when needed
 */
@Mixin(MinecraftServer.class)
public class MixinMinecraftServerPrepareStartRegion {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fibersync");

    @Inject(method = "prepareStartRegion", at = @At("HEAD"), cancellable = true)
    private void skipPrepareStartRegion(CallbackInfo ci) {
        LOGGER.info("Skipping prepareStartRegion() to avoid 1.21.11 compatibility issues");
        ci.cancel();
    }
}

