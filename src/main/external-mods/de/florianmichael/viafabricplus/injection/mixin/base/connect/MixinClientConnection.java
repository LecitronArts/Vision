/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.injection.mixin.base.connect;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.protocoltranslator.netty.ViaFabricPlusVLLegacyPipeline;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.network.CipherDecoder;
import net.minecraft.network.CipherEncoder;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.SampleLogger;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialoader.netty.CompressionReorderEvent;
import net.raphimc.vialoader.netty.VLLegacyPipeline;
import net.raphimc.vialoader.netty.VLPipeline;
import net.raphimc.vialoader.netty.viabedrock.PingEncapsulationCodec;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.crypto.Cipher;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(Connection.class)
public abstract class MixinClientConnection extends SimpleChannelInboundHandler<Packet<?>> /*implements IClientConnection*/ {

    @Shadow
    public Channel channel;

    @Shadow
    private boolean encrypted;

    @Shadow
    public abstract void channelActive(@NotNull ChannelHandlerContext context) throws Exception;

    @Unique
    private UserConnection viaFabricPlus$userConnection;

    @Unique
    private ProtocolVersion viaFabricPlus$serverVersion;

    @Unique
    private Cipher viaFabricPlus$decryptionCipher;


    @Inject(method = "setupCompression", at = @At("RETURN"))
    private void reorderCompression(int compressionThreshold, boolean rejectBad, CallbackInfo ci) {
        channel.pipeline().fireUserEventTriggered(CompressionReorderEvent.INSTANCE);
    } // ok

    @Inject(method = "setEncryptionKey", at = @At("HEAD"), cancellable = true)
    private void storeDecryptionCipher(Cipher decryptionCipher, Cipher encryptionCipher, CallbackInfo ci) {
        if (this.viaFabricPlus$serverVersion != null  /*This happens when opening a lan server and people are joining */ && this.viaFabricPlus$serverVersion.olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
            // Minecraft's encryption code is bad for us, we need to reorder the pipeline
            ci.cancel();

            // Minecraft 1.6.4 supports split encryption/decryption which means the server can only enable one side of the encryption
            // So we only enable the encryption side and later enable the decryption side if the 1.7 -> 1.6 protocol
            // tells us to do, therefore we need to store the cipher instance.
            this.viaFabricPlus$decryptionCipher = decryptionCipher;

            // Enabling the encryption side
            if (encryptionCipher == null) {
                throw new IllegalStateException("Encryption cipher is null");
            }

            this.encrypted = true;
            this.channel.pipeline().addBefore(VLLegacyPipeline.VIALEGACY_PRE_NETTY_LENGTH_REMOVER_NAME, "encrypt", new CipherEncoder(encryptionCipher));
        }
    } // ok

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        if (BedrockProtocolVersion.bedrockLatest.equals(this.viaFabricPlus$serverVersion)) { // Call channelActive manually when the channel is registered
            this.channelActive(ctx);
        }
    } // ok

    @WrapWithCondition(method = "channelActive", at = @At(value = "INVOKE", target = "Lio/netty/channel/SimpleChannelInboundHandler;channelActive(Lio/netty/channel/ChannelHandlerContext;)V", remap = false))
    private boolean dontCallChannelActiveTwice(SimpleChannelInboundHandler<Packet<?>> instance, ChannelHandlerContext channelHandlerContext) {
        return !BedrockProtocolVersion.bedrockLatest.equals(this.viaFabricPlus$serverVersion);
    } // ok

/*    @Inject(method = "connectToServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;", shift = At.Shift.BEFORE))
    private static void setTargetVersion(InetSocketAddress address, boolean useEpoll, SampleLogger packetSizeLog, CallbackInfoReturnable<Connection> cir, @Local Connection clientConnection) {
        // Set the target version stored in the PerformanceLog field to the ClientConnection instance
        if (packetSizeLog instanceof IPerformanceLog mixinPerformanceLog && mixinPerformanceLog.viaFabricPlus$getForcedVersion() != null) {
            ((IClientConnection) clientConnection).viaFabricPlus$setTargetVersion(mixinPerformanceLog.viaFabricPlus$getForcedVersion());
        }
    }*/ // ok

/*    @WrapWithCondition(method = "connectToServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setBandwidthLogger(Lnet/minecraft/util/SampleLogger;)V"))
    private static boolean dontSetPerformanceLog(Connection instance, SampleLogger log) {
        // We need to restore vanilla behaviour since we use the PerformanceLog as a way to store the target version
        return !(log instanceof IPerformanceLog mixinPerformanceLog) || mixinPerformanceLog.viaFabricPlus$getForcedVersion() == null;
    } */// ok

/*    @Inject(method = "connect", at = @At("HEAD"))
    private static void setTargetVersion(InetSocketAddress address, boolean useEpoll, Connection connection, CallbackInfoReturnable<ChannelFuture> cir) {
        ProtocolVersion targetVersion = ((IClientConnection) connection).viaFabricPlus$getTargetVersion();
        if (targetVersion == null) { // No server specific override
            targetVersion = ProtocolTranslator.getTargetVersion();
        }
        if (targetVersion == ProtocolTranslator.AUTO_DETECT_PROTOCOL) { // Auto-detect enabled (when pinging always use native version). Auto-detect is resolved in ConnectScreen mixin
            targetVersion = ProtocolTranslator.NATIVE_VERSION;
        }

        ((IClientConnection) connection).viaFabricPlus$setTargetVersion(targetVersion);
    } */// ok

/*    @WrapOperation(method = "connect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private static AbstractBootstrap<?, ?> useRakNetChannelFactory(Bootstrap instance, Class<? extends Channel> channelTypeClass, Operation<AbstractBootstrap<Bootstrap, Channel>> original, @Local(argsOnly = true) Connection clientConnection) {
        if (BedrockProtocolVersion.bedrockLatest.equals(((IClientConnection) clientConnection).viaFabricPlus$getTargetVersion())) {
            return instance.channelFactory(channelTypeClass == EpollSocketChannel.class ? RakChannelFactory.client(EpollDatagramChannel.class) : RakChannelFactory.client(NioDatagramChannel.class));
        } else {
            return original.call(instance, channelTypeClass);
        }
    }*/ // ok ?
    // TODO: 8/17/2024


/*    @Redirect(method = "connect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;connect(Ljava/net/InetAddress;I)Lio/netty/channel/ChannelFuture;", remap = false))
    private static ChannelFuture useRakNetPingHandlers(Bootstrap instance, InetAddress inetHost, int inetPort, @Local(argsOnly = true) Connection clientConnection, @Local(argsOnly = true) boolean isConnecting) {
        if (BedrockProtocolVersion.bedrockLatest.equals(((IClientConnection) clientConnection).viaFabricPlus$getTargetVersion()) && !isConnecting) {
            // Bedrock edition / RakNet has different handlers for pinging a server
            return instance.register().syncUninterruptibly().channel().bind(new InetSocketAddress(0)).addListeners(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE, (ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    f.channel().pipeline().replace(
                            VLPipeline.VIABEDROCK_FRAME_ENCAPSULATION_HANDLER_NAME,
                            ViaFabricPlusVLLegacyPipeline.VIABEDROCK_PING_ENCAPSULATION_HANDLER_NAME,
                            new PingEncapsulationCodec(new InetSocketAddress(inetHost, inetPort))
                    );
                    f.channel().pipeline().remove(VLPipeline.VIABEDROCK_PACKET_ENCAPSULATION_HANDLER_NAME);
                    f.channel().pipeline().remove("splitter");
                }
            });
        } else {
            return instance.connect(inetHost, inetPort);
        }
    } */// okk???

/*    @Override
    public void viaFabricPlus$setupPreNettyDecryption() {
        if (this.viaFabricPlus$decryptionCipher == null) {
            throw new IllegalStateException("Decryption cipher is null");
        }

        this.encrypted = true;
        // Enabling the decryption side for 1.6.4 if the 1.7 -> 1.6 protocol tells us to do
        this.channel.pipeline().addBefore(VLLegacyPipeline.VIALEGACY_PRE_NETTY_LENGTH_PREPENDER_NAME, "decrypt", new CipherDecoder(this.viaFabricPlus$decryptionCipher));
    } //*/

/*    @Override
    public UserConnection viaFabricPlus$getUserConnection() {
        return this.viaFabricPlus$userConnection;
    }

    @Override
    public void viaFabricPlus$setUserConnection(UserConnection userConnection) {
        this.viaFabricPlus$userConnection = userConnection;
    }

    @Override
    public ProtocolVersion viaFabricPlus$getTargetVersion() {
        return this.viaFabricPlus$serverVersion;
    }

    @Override
    public void viaFabricPlus$setTargetVersion(final ProtocolVersion serverVersion) {
        this.viaFabricPlus$serverVersion = serverVersion;
    }*/

}
