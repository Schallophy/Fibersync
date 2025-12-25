package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.AwaitingPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerList {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fibersync");

    @Shadow protected MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("HEAD"), cancellable = true)
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        try {
            var limbo = ((IServer) this.server).getLimbo(null);
            if (limbo != null) {
                LOGGER.info("Player {} connecting during Limbo phase", player.getGameProfile().name());
                limbo.onPlayerConnect(new AwaitingPlayer(limbo, player, connection), true);
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Error in MixinPlayerList.onPlayerConnect", e);
            e.printStackTrace();
        }
    }
}






