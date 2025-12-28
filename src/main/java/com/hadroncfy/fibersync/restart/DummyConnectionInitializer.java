package com.hadroncfy.fibersync.restart;

import com.hadroncfy.fibersync.FibersyncMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.server.MinecraftServer;

/**
 * 1.21 适配：虚假连接初始化器
 * 
 * 负责正确初始化虚假连接的协议状态，
 * 确保支持 1.21 引入的数据包打包（Packet Bundling）机制。
 */
public class DummyConnectionInitializer {
    
    /**
     * 初始化虚假连接的协议状态
     */
    public static void initializeDummyConnection(
            ClientConnection connection,
            MinecraftServer server,
            ServerDummyPlayHandler playHandler) {
        
        try {
            FibersyncMod.LOGGER.debug("Initializing dummy connection");
            
            // 配置入站状态（C2S - Client to Server）
            configureInboundState(connection, server, playHandler);
            
            // 配置出站状态（S2C - Server to Client）
            configureOutboundState(connection, server);
            
            FibersyncMod.LOGGER.debug("Dummy connection initialized successfully");
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to initialize dummy connection", e);
        }
    }
    
    /**
     * 配置入站网络状态（C2S）
     */
    private static void configureInboundState(
            ClientConnection connection,
            MinecraftServer server,
            ServerDummyPlayHandler playHandler) {
        
        try {
            var registryByteBufFactory = RegistryByteBuf.makeFactory(server.getRegistryManager());
            
            // 绑定 PLAY 状态的入站编解码器
            var inboundState = PlayStateFactories.C2S.bind(registryByteBufFactory);
            
            // 过渡到 PLAY 状态并设置数据包监听器
            connection.transitionInbound(inboundState, playHandler);
            
            FibersyncMod.LOGGER.debug("Inbound state configured");
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to configure inbound state", e);
        }
    }
    
    /**
     * 配置出站网络状态（S2C）
     */
    private static void configureOutboundState(
            ClientConnection connection,
            MinecraftServer server) {
        
        try {
            var registryByteBufFactory = RegistryByteBuf.makeFactory(server.getRegistryManager());
            
            // 绑定 PLAY 状态的出站编解码器
            var outboundState = PlayStateFactories.S2C.bind(registryByteBufFactory);
            
            // 过渡到 PLAY 状态
            connection.transitionOutbound(outboundState);
            
            FibersyncMod.LOGGER.debug("Outbound state configured");
            
        } catch (Exception e) {
            FibersyncMod.LOGGER.error("Failed to configure outbound state", e);
        }
    }
}
