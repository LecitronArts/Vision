/**
 * @Author Empty_0
 * @Date 2024/3/24
 */
package dev.vision.events;

import me.empty.api.event.Cancelable;

public class EventSlowChange extends Cancelable {
    private float moveStrafe;
    private float moveForward;

    public EventSlowChange(float moveStrafe, float moveForward) {
        this.moveStrafe = moveStrafe;
        this.moveForward = moveForward;
    }

    public float getMoveStrafe() {
        return moveStrafe;
    }

    public void setMoveStrafe(float moveStrafe) {
        this.moveStrafe = moveStrafe;
    }

    public float getMoveForward() {
        return moveForward;
    }

    public void setMoveForward(float moveForward) {
        this.moveForward = moveForward;
    }

    public void setSlow(float speed) {
        this.moveStrafe = speed;
        this.moveForward = speed;
    }
}
