package com.hadroncfy.fibersync.restart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.interfaces.IPlayer;
import com.hadroncfy.fibersync.interfaces.IPlayerManager;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.util.copy.FileOperationProgressListener;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.util.ProgressListener;
import net.minecraft.entity.EntityPosition;

public class Limbo {
    public final Set<RegistryKey<World>> world_keys;
    private final List<AwaitingPlayer> players = new ArrayList<>();
    private final MinecraftServer server;
    private final RollBackProgressListener rollBackProgressListener = new RollBackProgressListener(this);

    public Limbo(MinecraftServer server) {
        this.server = server;
        this.world_keys = new HashSet<>();
        for (var k: server.getWorldRegistryKeys()) {
            this.world_keys.add(k);
        }
    }

    public MinecraftServer getServer(){
        return server;
    }

    public void start() {
        for (ServerPlayerEntity player : new ArrayList<>(server.getPlayerManager().getPlayerList())) {
            server.getPlayerManager().remove(player);
            var p = new AwaitingPlayer(this, player, player.networkHandler.connection);
            onPlayerConnect(p, false);
        }
        ((IServer) this.server).setLimbo(null, this);
    }

    public ProgressListener getWorldGenListener(){
        return rollBackProgressListener;
    }

    public FileOperationProgressListener getFileCopyListener(){
        return rollBackProgressListener;
    }

    public void onPlayerConnect(AwaitingPlayer p, boolean sendJoin){
        if (sendJoin) {
            ServerWorld overworld = this.server.getOverworld();
            // Create CommonPlayerSpawnInfo manually since createCommonSpawnInfo no longer exists
            CommonPlayerSpawnInfo spawnInfo = new CommonPlayerSpawnInfo(
                overworld.getDimensionEntry(),
                overworld.getRegistryKey(),
                overworld.getSeed(),
                GameMode.SPECTATOR,
                null, // lastGameMode
                false, // isDebug
                overworld.isFlat(),
                Optional.empty(), // lastDeathLocation
                0, // portalCooldown
                overworld.getSeaLevel()
            );
            p.connection.send(new GameJoinS2CPacket(
                0,
                false,
                this.world_keys,
                20,
                10,
                10,
                false,
                false,
                false,
                spawnInfo,
                false
            ));
        }
        PlayerAbilities abilities = new PlayerAbilities();
        abilities.allowFlying = true;
        abilities.allowModifyWorld = false;
        abilities.invulnerable = true;
        abilities.flying = true;
        abilities.creativeMode = false;
        p.connection.send(new PlayerAbilitiesS2CPacket(abilities));
        
        // Send player position
        Vec3d pos = new Vec3d(0, 0, 0);
        EntityPosition entityPos = new EntityPosition(pos, pos, 0, 0);
        p.connection.send(new PlayerPositionLookS2CPacket(0, entityPos, Collections.emptySet()));
        
        rollBackProgressListener.onPlayerConnected(p);

        FibersyncMod.LOGGER.info("Player {} joined limbo", p.profile.name());
        addPlayer(p);
    }

    private synchronized void addPlayer(AwaitingPlayer p){
        players.add(p);
    }

    public void end() {
        final PlayerManager playerManager = server.getPlayerManager();
        final IPlayerManager pm = (IPlayerManager) playerManager;
        final ServerWorld dummy = server.getOverworld();

        this.removeRemovedPlayers();

        rollBackProgressListener.end();

        pm.reset(null);
        ((IServer) this.server).setLimbo(null, null);

        if (server.isSingleplayer() && players.isEmpty()){
            FibersyncMod.LOGGER.info("Stopping server as the server has no players");
        } else {
            pm.setShouldRefreshScreen(null, true);
            for (AwaitingPlayer player : players) {
                var playerEntity = player.entity;
                if (playerEntity == null) {
                    // Create ServerPlayerEntity directly since createPlayer no longer exists
                    playerEntity = new ServerPlayerEntity(server, dummy, player.profile, player.syncedOptions);
                }
                playerEntity.setWorld(dummy);
                
                // Only call reset if we created a new entity
                if (player.entity == null) {
                    ((IPlayer)playerEntity).reset(null);
                }
                
                // Ensure player abilities allow world modification
                PlayerAbilities abilities = playerEntity.getAbilities();
                abilities.allowModifyWorld = true;

                playerManager.onPlayerConnect(player.connection, playerEntity, player.clientData);
            }
            pm.setShouldRefreshScreen(null, false);
        }
        players.clear();
    }

    public void sendToAll(Packet<?> packet) {
        for (AwaitingPlayer player : players) {
            // 1.21 适配：使用 ClientConnection.send() 确保数据包正确地通过 Pipeline
            // 这会自动处理数据包的打包和排队
            player.connection.send(packet);
        }
    }

    public void broadcast(Text txt){
        sendToAll(new GameMessageS2CPacket(txt, false));
    }

    public void tick() {
        this.removeRemovedPlayers();
        this.server.getNetworkIo().tick();
    }

    public synchronized void removeRemovedPlayers(){
        for (Iterator<AwaitingPlayer> iterator = players.iterator(); iterator.hasNext();){
            AwaitingPlayer p = iterator.next();
            if (p.removed){
                iterator.remove();
                FibersyncMod.LOGGER.info("Player {} left limbo", p.profile.name());
            }
        }
    }
}
