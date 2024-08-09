package com.velum.videotape.mixin;

import com.velum.videotape.event.ClientEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public class ClientTickMixin {
    public ClientTickMixin() {
    }

    @Inject(
        at = {@At("RETURN")},
        method = {"tick"}
    )
    public void onEndTick(CallbackInfo callbackInfo) {
        ClientEventHandler.onClientTick();
    }
}
