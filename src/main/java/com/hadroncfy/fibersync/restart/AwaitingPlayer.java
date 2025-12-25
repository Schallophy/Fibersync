package com.hadroncfy.fibersync.restart;

import com.mojang.authlib.GameProfile;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;

public class AwaitingPlayer {
    public final ServerPlayerEntity entity;
    public final ClientConnection connection;
    public final ServerDummyPlayHandler handler;
    public final GameProfile profile;
    public final SyncedClientOptions syncedOptions;
    public final ConnectedClientData clientData;
    public boolean removed = false;

    public AwaitingPlayer(Limbo limbo, ServerPlayerEntity entity, ClientConnection connection){
        this.entity = entity;
        this.connection = connection;
        this.profile = entity.getGameProfile();
        this.syncedOptions = entity.getClientOptions();
        this.clientData = new ConnectedClientData(profile, 0, syncedOptions, false);
        if (entity != null) {
            entity.setWorld(null);
        }
        this.handler = new ServerDummyPlayHandler(limbo, this);
    }

    public AwaitingPlayer(Limbo limbo, GameProfile profile, ClientConnection connection){
        this.entity = null;
        this.connection = connection;
        this.profile = profile;
        this.syncedOptions = SyncedClientOptions.createDefault();
        this.clientData = new ConnectedClientData(profile, 0, syncedOptions, false);
        this.handler = new ServerDummyPlayHandler(limbo, this);
    }
}
