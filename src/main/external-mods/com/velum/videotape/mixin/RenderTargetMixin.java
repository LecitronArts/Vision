//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.velum.videotape.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.velum.videotape.event.ClientEventHandler;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(
    value = {RenderTarget.class},
    remap = false
)
public abstract class RenderTargetMixin {
    @Shadow(
        remap = true
    )
    protected int depthBufferId;
    @Shadow(
        remap = true
    )
    protected int colorTextureId;
    @Shadow(
        remap = true
    )
    public int frameBufferId;

    public RenderTargetMixin() {
    }

    public void finalize() throws Throwable {
        try {
            if (this.depthBufferId > -1 || this.colorTextureId > -1 || this.frameBufferId > -1) {
                ClientEventHandler.queue.add(new Vec3i(this.depthBufferId, this.colorTextureId, this.frameBufferId));
            }
        } finally {
            super.finalize();
        }

    }
}
