package com.hadroncfy.fibersync.restart;

import com.hadroncfy.fibersync.FibersyncMod;

import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.common.ClientOptionsC2SPacket;
import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class ServerDummyPlayHandler implements ServerPlayPacketListener, TickablePacketListener {
    private final AwaitingPlayer player;
    private final Limbo limbo;

    private boolean waitingForKeepAlive;
    private long lastKeepAliveTime = Util.getMeasuringTimeMs();
    private long keepAliveId;


    public ServerDummyPlayHandler(Limbo limbo, AwaitingPlayer player){
        this.player = player;
        this.limbo = limbo;
        
        // 1.21 适配：使用 DummyConnectionInitializer 正确初始化虚假连接
        // 这确保 Netty Pipeline 包含所有必要的处理器，包括 PacketBundleHandler
        DummyConnectionInitializer.initializeDummyConnection(
            player.connection,
            limbo.getServer(),
            this
        );

        final PlayerAbilities ab = new PlayerAbilities();
        ab.flying = true;
        player.connection.send(new PlayerAbilitiesS2CPacket(ab));
    }

    private void disconnect(Text reason){
        final ClientConnection connection = player.connection;
        connection.send(new DisconnectS2CPacket(reason));
        connection.disconnect(new DisconnectionInfo(reason));
    }

    @Override
    public void tick(){
        long l = Util.getMeasuringTimeMs();
        if (l - lastKeepAliveTime >= 15000L) {
           if (waitingForKeepAlive) {
              this.disconnect(Text.translatable("disconnect.timeout"));
           } else {
              waitingForKeepAlive = true;
              lastKeepAliveTime = l;
              keepAliveId = l;
              player.connection.send(new KeepAliveS2CPacket(keepAliveId));
           }
        }
    }

    @Override
    public void onDisconnected(DisconnectionInfo info) {
        player.removed = true;
        FibersyncMod.LOGGER.info("{} lost connection: {}", player.profile.name(), info.reason().getString());
    }

    @Override
    public void onHandSwing(HandSwingC2SPacket packet) {
    }

    @Override
    public void onChatMessage(ChatMessageC2SPacket packet) {
        final String msg = packet.chatMessage();
        Text text = Text.translatable("chat.type.text", player.profile.name(), msg);
        limbo.broadcast(text);
    }

    @Override
    public void onClientStatus(ClientStatusC2SPacket packet) {
    }

    @Override
    public void onButtonClick(ButtonClickC2SPacket packet) {
    }

    @Override
    public void onClickSlot(ClickSlotC2SPacket packet) {
    }

    @Override
    public void onCraftRequest(CraftRequestC2SPacket packet) {
    }

    @Override
    public void onCloseHandledScreen(CloseHandledScreenC2SPacket packet) {
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityC2SPacket packet) {
    }

    @Override
    public void onKeepAlive(KeepAliveC2SPacket packet) {
        if (this.waitingForKeepAlive && packet.getId() == this.keepAliveId) {
            this.waitingForKeepAlive = false;
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveC2SPacket packet) {
    }

    @Override
    public void onUpdatePlayerAbilities(UpdatePlayerAbilitiesC2SPacket packet) {
    }

    @Override
    public void onPlayerAction(PlayerActionC2SPacket packet) {
    }

    @Override
    public void onClientCommand(ClientCommandC2SPacket packet) {
    }

    @Override
    public void onPlayerInput(PlayerInputC2SPacket packet) {
    }

    @Override
    public void onUpdateSelectedSlot(UpdateSelectedSlotC2SPacket packet) {
    }

    @Override
    public void onCreativeInventoryAction(CreativeInventoryActionC2SPacket packet) {
    }

    @Override
    public void onUpdateSign(UpdateSignC2SPacket packet) {
    }

    @Override
    public void onPlayerInteractBlock(PlayerInteractBlockC2SPacket packet) {
    }

    @Override
    public void onPlayerInteractItem(PlayerInteractItemC2SPacket packet) {
    }

    @Override
    public void onSpectatorTeleport(SpectatorTeleportC2SPacket packet) {
    }

    @Override
    public void onBoatPaddleState(BoatPaddleStateC2SPacket packet) {
    }

    @Override
    public void onVehicleMove(VehicleMoveC2SPacket packet) {
    }

    @Override
    public void onTeleportConfirm(TeleportConfirmC2SPacket packet) {
    }

    @Override
    public void onRecipeBookData(RecipeBookDataC2SPacket packet) {
    }

    @Override
    public void onAdvancementTab(AdvancementTabC2SPacket packet) {
    }

    @Override
    public void onRequestCommandCompletions(RequestCommandCompletionsC2SPacket packet) {
    }

    @Override
    public void onUpdateCommandBlock(UpdateCommandBlockC2SPacket packet) {
    }

    @Override
    public void onUpdateCommandBlockMinecart(UpdateCommandBlockMinecartC2SPacket packet) {
    }

    @Override
    public void onRenameItem(RenameItemC2SPacket packet) {
    }

    @Override
    public void onUpdateBeacon(UpdateBeaconC2SPacket packet) {
    }

    @Override
    public void onUpdateStructureBlock(UpdateStructureBlockC2SPacket packet) {
    }

    @Override
    public void onSelectMerchantTrade(SelectMerchantTradeC2SPacket packet) {
    }

    @Override
    public void onBookUpdate(BookUpdateC2SPacket packet) {
    }

    @Override
    public void onQueryEntityNbt(QueryEntityNbtC2SPacket packet) {
    }

    @Override
    public void onQueryBlockNbt(QueryBlockNbtC2SPacket packet) {
    }

    @Override
    public void onUpdateJigsaw(UpdateJigsawC2SPacket packet) {
    }

    @Override
    public void onUpdateDifficulty(UpdateDifficultyC2SPacket packet) {
    }

    @Override
    public void onUpdateDifficultyLock(UpdateDifficultyLockC2SPacket packet) {
    }

    @Override
    public void onJigsawGenerating(JigsawGeneratingC2SPacket packet) {
    }

    @Override
    public void onRecipeCategoryOptions(RecipeCategoryOptionsC2SPacket packet) {
    }

    @Override
    public void onPong(CommonPongC2SPacket packet) {
    }

    @Override
    public boolean isConnectionOpen() {
        return player.connection.isOpen();
    }

    @Override
    public void onCommandExecution(CommandExecutionC2SPacket packet) {
    }

    @Override
    public void onMessageAcknowledgment(MessageAcknowledgmentC2SPacket packet) {
    }

    @Override
    public void onChatCommandSigned(ChatCommandSignedC2SPacket packet) {
    }

    @Override
    public void onPlayerSession(PlayerSessionC2SPacket packet) {
    }

    @Override
    public void onAcknowledgeChunks(AcknowledgeChunksC2SPacket packet) {
    }

    @Override
    public void onSlotChangedState(SlotChangedStateC2SPacket packet) {
    }

    @Override
    public void onClientTickEnd(ClientTickEndC2SPacket packet) {
    }

    @Override
    public void onBundleItemSelected(BundleItemSelectedC2SPacket packet) {
    }

    @Override
    public void onCustomPayload(CustomPayloadC2SPacket packet) {
    }

    @Override
    public void onClientOptions(ClientOptionsC2SPacket packet) {
    }

    @Override
    public void onPickItemFromBlock(PickItemFromBlockC2SPacket packet) {
    }

    @Override
    public void onPickItemFromEntity(PickItemFromEntityC2SPacket packet) {
    }

    @Override
    public void onDebugSubscriptionRequest(DebugSubscriptionRequestC2SPacket packet) {
    }

    @Override
    public void onChangeGameMode(ChangeGameModeC2SPacket packet) {
    }

    @Override
    public void onAcknowledgeReconfiguration(AcknowledgeReconfigurationC2SPacket packet) {
    }

    @Override
    public void onTestInstanceBlockAction(TestInstanceBlockActionC2SPacket packet) {
    }

    @Override
    public void onSetTestBlock(SetTestBlockC2SPacket packet) {
    }

    @Override
    public void onPlayerLoaded(PlayerLoadedC2SPacket packet) {
    }

    @Override
    public void onCustomClickAction(CustomClickActionC2SPacket packet) {
    }

    @Override
    public void onResourcePackStatus(ResourcePackStatusC2SPacket packet) {
    }

    @Override
    public void onCookieResponse(CookieResponseC2SPacket packet) {
    }

    @Override
    public void onQueryPing(QueryPingC2SPacket packet) {
    }
}
