package com.hadroncfy.fibersync.mixin;

import java.util.Map;
import java.util.UUID;

import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.interfaces.Unit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.world.GameMode;

@Mixin(PlayerManager.class)
public class MixinPlayerManager implements IPlayerManager {
    @Unique boolean shouldRefreshScreen;

    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private Map<UUID, ServerStatHandler> statisticsMap;
    @Shadow @Final private Map<UUID, PlayerAdvancementTracker> advancementTrackers;

    @Inject(method = "onPlayerConnect", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
        ordinal = 1
    ))
    private void onSendGameJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci){
        if (this.shouldRefreshScreen) {
            GameMode gmode = player.interactionManager.getGameMode();
            var world = player.getEntityWorld();

            // Send these two packets to prevent the client from being stuck in the downloading terrain screen
            // Create CommonPlayerSpawnInfo manually
            var spawnInfo = new net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo(
                world.getDimensionEntry(),
                world.getRegistryKey(),
                world.getSeed(),
                gmode,
                null, // lastGameMode
                false, // isDebug
                world.isFlat(),
                java.util.Optional.empty(), // lastDeathLocation
                0, // portalCooldown
                world.getSeaLevel()
            );
            
            connection.send(new PlayerRespawnS2CPacket(spawnInfo, (byte) 0));
            connection.send(new PlayerRespawnS2CPacket(spawnInfo, (byte) 0));
            
            // Send player abilities to ensure client has correct ability state
            PlayerAbilities abilities = player.getAbilities();
            connection.send(new PlayerAbilitiesS2CPacket(abilities));
        }
        var progress_bar = ((IServer) this.server).getBackupCommandContext(null).progress_bar.get();
        if (progress_bar != null) {
            progress_bar.addPlayer(player);
        }
    }

    @Override
    public void setShouldRefreshScreen(Unit u, boolean bl) {
        shouldRefreshScreen = bl;
    }

    @Override
    public void reset(Unit u) {
        this.statisticsMap.clear();
        this.advancementTrackers.clear();
    }
}
