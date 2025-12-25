package com.hadroncfy.fibersync.mixin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hadroncfy.fibersync.FibersyncMod;
import com.hadroncfy.fibersync.command.BackupCommand;
import com.mojang.brigadier.ParseResults;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class MixinServerPlayNetworkHandler {
    private static final Pattern PREFIX = Pattern.compile("^[^ ]+");

    @Shadow public ServerPlayerEntity player;
    @Shadow @Final private MinecraftServer server;

    @Shadow
    public abstract ParseResults<ServerCommandSource> parse(String command);

    @Inject(method = "handleDecoratedMessage", at = @At("HEAD"))
    private void onChat(SignedMessage message, CallbackInfo ci){
        String msg = message.getContent().getString();
        Matcher m = PREFIX.matcher(msg);
        if (m.find()){
            String prefix = m.group();
            if (FibersyncMod.getConfig().alternativeCmdPrefix.contains(prefix)){
                var cmd = BackupCommand.NAME + msg.substring(m.end());
                this.server.submit(() -> this.server.getCommandManager().execute(this.parse(cmd), cmd));
            }
        }
    }
}
