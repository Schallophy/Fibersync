package com.hadroncfy.fibersync.restart;

import com.hadroncfy.fibersync.FibersyncMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.state.ConfigurationStateFactories;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.server.MinecraftServer;
import io.netty.channel.Channel;

/**
 * 1.21 适配：虚假连接初始化器
 * 
 * 负责正确初始化虚假连接的 Netty Pipeline 和协议状态，
 * 确保支持 1.21 引入的数据包打包（Packet Bundling）机制。
 */
public class DummyConnectionInitializer {
    
    /**
     * 初始化虚假连接的协议状态
     * 
     * 1.21 中的连接状态流程：
     * HANDSHAKING -> LOGIN -> CONFIGURATION -> PLAY
     * 
     * 对于虚假连接，我们需要直接跳到 PLAY 状态，
     * 但必须确保 Pipeline 已正确配置以支持打包机制。
     */
    public static void initializeDummyConnection(
            ClientConnection connection,
            MinecraftServer server,
            ServerDummyPlayHandler playHandler) {
        
        try {
            // 第一步：验证 Netty Channel 和 Pipeline
            Channel channel = connection.getChannel();
            if (channel == null || !channel.isActive()) {
                FibersyncMod.LOGGER.error("Cannot initialize dummy connection: channel is null or inactive");
                return;
            }
            
            FibersyncMod.LOGGER.debug("Initializing dummy connection pipeline for {}", 
                connection.getAddress());
            
            // 第二步：配置入站状态（C2S - Client to Server）
            // 这允许连接接收来自客户端的数据包
            configureInboundState(connection, server, playHandler);
            
            // 第三步：配置出站状态（S2C - Server to Client）
            // 这允许连接发送数据包到客户端
            configureOutboundState(connection, server);
            
            FibersyncMod.LOGGER.debug("Dummy connection pipeline initialized successfully");
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to initialize dummy connection", e);
        }
    }
    
    /**
     * 配置入站网络状态（C2S）
     * 
     * 这设置了服务器接收客户端数据包的编解码器。
     */
    private static void configureInboundState(
            ClientConnection connection,
            MinecraftServer server,
            ServerDummyPlayHandler playHandler) {
        
        try {
            var registryByteBufFactory = RegistryByteBuf.makeFactory(server.getRegistryManager());
            
            var packetCodecModifierContext = new PlayStateFactories.PacketCodecModifierContext() {
                @Override
                public boolean isInCreativeMode() {
                    return false;
                }
            };
            
            // 绑定 PLAY 状态的入站编解码器
            var inboundState = PlayStateFactories.C2S.bind(
                registryByteBufFactory,
                packetCodecModifierContext
            );
            
            // 过渡到 PLAY 状态并设置数据包监听器
            connection.transitionInbound(inboundState, playHandler);
            
            FibersyncMod.LOGGER.debug("Inbound state configured for dummy connection");
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to configure inbound state", e);
        }
    }
    
    /**
     * 配置出站网络状态（S2C）
     * 
     * 这设置了服务器发送数据包到客户端的编解码器。
     * 1.21 中的出站状态包含 PacketBundleHandler，
     * 用于处理数据包打包机制。
     */
    private static void configureOutboundState(
            ClientConnection connection,
            MinecraftServer server) {
        
        try {
            var registryByteBufFactory = RegistryByteBuf.makeFactory(server.getRegistryManager());
            
            var packetCodecModifierContext = new PlayStateFactories.PacketCodecModifierContext() {
                @Override
                public boolean isInCreativeMode() {
                    return false;
                }
            };
            
            // 绑定 PLAY 状态的出站编解码器
            // 这会自动包含 PacketBundleHandler
            var outboundState = PlayStateFactories.S2C.bind(
                registryByteBufFactory,
                packetCodecModifierContext
            );
            
            // 过渡到 PLAY 状态
            connection.transitionOutbound(outboundState);
            
            FibersyncMod.LOGGER.debug("Outbound state configured for dummy connection");
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to configure outbound state", e);
        }
    }
    
    /**
     * 验证连接的协议状态
     * 
     * 用于调试和验证虚假连接是否正确初始化。
     */
    public static boolean verifyConnectionState(ClientConnection connection) {
        try {
            Channel channel = connection.getChannel();
            if (channel == null || !channel.isActive()) {
                FibersyncMod.LOGGER.warn("Connection channel is not active");
                return false;
            }
            
            // 检查 Pipeline 中是否存在必要的处理器
            var pipeline = channel.pipeline();
            
            // 检查是否有编解码器处理器
            boolean hasCodec = pipeline.get("packet_decoder") != null || 
                              pipeline.get("packet_encoder") != null;
            
            if (!hasCodec) {
                FibersyncMod.LOGGER.warn("Connection pipeline missing codec handlers");
                return false;
            }
            
            FibersyncMod.LOGGER.debug("Connection state verified successfully");
            return true;
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to verify connection state", e);
            return false;
        }
    }
}
