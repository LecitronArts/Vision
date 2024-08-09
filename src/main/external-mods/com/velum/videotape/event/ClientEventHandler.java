package com.velum.videotape.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.Vec3i;

public class ClientEventHandler {
    public static ConcurrentLinkedQueue<Vec3i> queue = new ConcurrentLinkedQueue();

    public ClientEventHandler() {
    }

    public static void onClientTick() {
        boolean done = false;
        int counter = 0;

        while(!queue.isEmpty() && counter++ < 20) {
            if (!done) {
                GlStateManager._bindTexture(0);
                GlStateManager._glBindFramebuffer(36160, 0);
                done = true;
            }

            Vec3i ids = (Vec3i)queue.poll();
            if (ids != null) {
                if (ids.getX() > -1) {
                    TextureUtil.releaseTextureId(ids.getX());
                }

                if (ids.getY() > -1) {
                    TextureUtil.releaseTextureId(ids.getY());
                }

                if (ids.getZ() > -1) {
                    GlStateManager._glBindFramebuffer(36160, 0);
                    GlStateManager._glDeleteFramebuffers(ids.getZ());
                }
            }
        }

    }
}
