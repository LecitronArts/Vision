package dev.vision.util;

import dev.vision.interfaces.InstanceMinecraft;

public class MovementUtils implements InstanceMinecraft {
    public static boolean isMoving() {
        assert mc.player != null;
        return mc.player.input.leftImpulse != 0 || mc.player.input.forwardImpulse != 0;
    }
}
