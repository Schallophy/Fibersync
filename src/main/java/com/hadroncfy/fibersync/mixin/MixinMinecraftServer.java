package com.hadroncfy.fibersync.mixin;

import java.io.IOException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.hadroncfy.fibersync.command.BackupCommandContext;
import com.hadroncfy.fibersync.interfaces.IServer;
import com.hadroncfy.fibersync.interfaces.IServerChunkManager;
import com.hadroncfy.fibersync.interfaces.IServerScoreboard;
import com.hadroncfy.fibersync.interfaces.Unit;
import com.hadroncfy.fibersync.restart.IReloadListener;
import com.hadroncfy.fibersync.restart.Limbo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.util.ProgressListener;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer extends ReentrantThreadExecutor<ServerTask> implements IServer {
    public MixinMinecraftServer(String string) {
        super(string);
    }

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("Fibersync");

    @Shadow @Final protected LevelStorage.Session session;
    @Shadow @Final private ServerNetworkIo networkIo;
    @Shadow @Final private ResourcePackManager dataPackManager;
    @Shadow private int ticks;
    @Shadow private volatile boolean running;

    // things that needs reseting
    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> worlds;
    @Shadow @Final private ServerScoreboard scoreboard;
    @Shadow @Mutable protected SaveProperties saveProperties;

    @Shadow
    public abstract void prepareStartRegion(ProgressListener progressListener);

    @Shadow
    protected abstract void createWorlds(ProgressListener progressListener);

    @Shadow
    protected abstract void updateDifficulty();

    @Unique private BackupCommandContext commandContext = new BackupCommandContext(() -> this.session.getDirectoryName());
    @Unique private Limbo limbo;

    @Unique private IReloadListener reloadCB;
    @Unique private Runnable tickTask;
    @Unique private int tickTaskPeriod;
    @Unique private int tickBase;

    @Unique
    private void loadWorld(ProgressListener startRegionListener) {
        LOGGER.info("loadWorld() started");
        this.createWorlds(startRegionListener);
        LOGGER.info("createWorlds() completed");
        this.updateDifficulty();
        LOGGER.info("updateDifficulty() completed");
        this.prepareStartRegion(startRegionListener);
        LOGGER.info("prepareStartRegion() completed");
    }


    @Override
    public void reloadAll(Unit u, IReloadListener listener){
        if (reloadCB == null) {
            reloadCB = listener;
        } else {
            throw new IllegalStateException("A reloading task is already in progress!");
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        if (reloadCB != null) {
            LOGGER.info("onTick: reloadCB is not null, starting reload process");
            final Limbo limbo = new Limbo((MinecraftServer) (Object) this);

            limbo.start();
            LOGGER.info("Purging worlds");
            for (ServerWorld world : worlds.values()) {
                try {
                    world.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            worlds.clear();

            {
                this.tasks.clear();
                this.executionsInProgress = 0;
            }

            final var finished = new boolean[]{false};
            final var reload_thrd = new Thread(() -> {
                LOGGER.info("Reload thread started");
                reloadCB.onReload(limbo);
                finished[0] = true;
                LOGGER.info("Reload thread finished");
            });
            reload_thrd.start();
            while (!finished[0]) {
                limbo.removeRemovedPlayers();
                try {
                    this.networkIo.tick();
                } catch (Exception e) {
                    LOGGER.error("Network error during limbo", e);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            LOGGER.info("Reloading");
            this.resetServer();
            LOGGER.info("resetServer() completed");
            this.loadWorld(limbo.getWorldGenListener());
            LOGGER.info("loadWorld() completed");
            limbo.end();
            LOGGER.info("limbo.end() completed");

            reloadCB.onReloadDone();
            reloadCB = null;

            // Setup spawn info for all worlds
            for (var world: this.worlds.values()) {
                ((IServerChunkManager) world.getChunkManager()).setupSpawnInfo(null);
            }

            // Note: setupSpawnInfo is called automatically during world creation
            // Commenting out to avoid potential issues during initial world generation
            // for (var world: this.worlds.values()) {
            //     ((IServerChunkManager) world.getChunkManager()).setupSpawnInfo(null);
            // }
        }
    }

    @Unique
    private void resetServer() {
        ((IServerScoreboard) this.scoreboard).reset(null);
    }

    @Inject(method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/MinecraftServer;tickWorlds(Ljava/util/function/BooleanSupplier;)V"
    ))
    private void onTickWorld(BooleanSupplier booleanSupplier, CallbackInfo ci){
        if (tickTask != null && (ticks - tickBase) % tickTaskPeriod == 0){
            tickTask.run();
        }
    }

    @Override
    public void setTickTask(Unit u, Runnable task, int period) {
        tickTask = task;
        tickTaskPeriod = period;
        tickBase = ticks;
    }

    @Override
    public BackupCommandContext getBackupCommandContext(Unit u) {
        return commandContext;
    }

    @Override
    public Limbo getLimbo(Unit u) {
        return this.limbo;
    }

    @Override
    public void setLimbo(Unit u, Limbo limbo) {
        this.limbo = limbo;
    }
}
