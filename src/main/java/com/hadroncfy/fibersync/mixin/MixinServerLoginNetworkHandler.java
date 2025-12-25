package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.restart.AwaitingPlayer;
import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(ServerLoginNetworkHandler.class)
public class MixinServerLoginNetworkHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Fibersync");

    @Shadow @Final MinecraftServer server;
    @Shadow @Final ClientConnection connection;
    @Shadow GameProfile profile;

    /**
     * In 1.21.11, player login is handled by PlayerList.placeNewPlayer()
     * We need to intercept at that point instead of in ServerLoginNetworkHandler
     * 
     * This Mixin is kept for reference but the actual interception happens in MixinPlayerList
     */
}





