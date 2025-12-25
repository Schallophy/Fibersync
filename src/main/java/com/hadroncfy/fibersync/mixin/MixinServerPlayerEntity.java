package com.hadroncfy.fibersync.mixin;

import com.hadroncfy.fibersync.interfaces.IPlayer;
import com.hadroncfy.fibersync.interfaces.Unit;
import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerRecipeBook;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements IPlayer {

    public MixinServerPlayerEntity(World world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow @Final
    public MinecraftServer server;
    @Shadow @Mutable
    private ServerRecipeBook recipeBook;
    @Shadow @Mutable
    private ServerStatHandler statHandler;
    @Shadow @Mutable
    private PlayerAdvancementTracker advancementTracker;

    @Override
    public void reset(Unit u) {
        this.unsetRemoved();
        // Create a new ServerRecipeBook with a DisplayCollector that uses the ServerRecipeManager
        recipeBook = new ServerRecipeBook((recipeKey, adder) -> {
            server.getRecipeManager().forEachRecipeDisplay(recipeKey, adder);
        });
        statHandler = server.getPlayerManager().createStatHandler((ServerPlayerEntity)(Object)this);
        advancementTracker = server.getPlayerManager().getAdvancementTracker((ServerPlayerEntity)(Object)this);
        ((ContainerAccessor)currentScreenHandler).getListeners().clear();// avoid inventory desync
        
        // 恢复玩家的能力 - 允许修改世界
        try {
            ServerPlayerEntity self = (ServerPlayerEntity)(Object)this;
            if (self.getAbilities() != null) {
                self.getAbilities().allowModifyWorld = true;
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    @Override
    public void resetWorld(Unit u) {
        this.setWorld(null);
    }
}
