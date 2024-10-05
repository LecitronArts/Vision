/**
 * @Author Empty_0
 * @Date 2024/3/24
 */
package dev.vision.events;

import me.empty.api.event.Cancelable;

public class EventSlowChange extends Cancelable {
    private float leftImpulse;
    private float forwardImpulse;

    public EventSlowChange(float leftImpulse, float forwardImpulse) {
        this.leftImpulse = leftImpulse;
        this.forwardImpulse = forwardImpulse;
    }

    public float getLeftImpulse() {
        return leftImpulse;
    }

    public void setLeftImpulse(float leftImpulse) {
        this.leftImpulse = leftImpulse;
    }

    public float getForwardImpulse() {
        return forwardImpulse;
    }

    public void setForwardImpulse(float forwardImpulse) {
        this.forwardImpulse = forwardImpulse;
    }

    public void setSlow(float speed) {
        this.leftImpulse = speed;
        this.forwardImpulse = speed;
    }
}
