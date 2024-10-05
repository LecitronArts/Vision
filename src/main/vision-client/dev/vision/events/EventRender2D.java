package dev.vision.events;

import com.mojang.blaze3d.vertex.PoseStack;
import me.empty.api.event.Cancelable;

public class EventRender2D extends Cancelable {
    private final PoseStack poseStack;
    private final float partialTicks;

    public EventRender2D(PoseStack poseStack, float partialTicks) {
        this.poseStack = poseStack;
        this.partialTicks = partialTicks;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }
}
