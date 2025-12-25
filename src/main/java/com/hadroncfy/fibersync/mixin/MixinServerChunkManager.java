package com.hadroncfy.fibersync.mixin;

import java.util.List;
import java.util.function.Consumer;

import com.hadroncfy.fibersync.interfaces.IServerChunkManager;
import com.hadroncfy.fibersync.interfaces.Unit;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.world.SpawnDensityCapper;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager implements IServerChunkManager {
    @Shadow @Final public ServerChunkLoadingManager chunkLoadingManager;
    @Shadow private List<WorldChunk> spawningChunks;
    @Shadow private SpawnHelper.Info spawnInfo;
    @Shadow ServerWorld world;

    @Shadow
    abstract void ifChunkLoaded(long pos, Consumer<WorldChunk> chunkConsumer);

    @Override
    public void setupSpawnInfo(Unit u) {
        this.spawnInfo = SpawnHelper.setupSpawn(
            this.spawningChunks.size(),
            this.world.iterateEntities(),
            this::ifChunkLoaded,
            new SpawnDensityCapper(this.chunkLoadingManager)
        );
    }
}
