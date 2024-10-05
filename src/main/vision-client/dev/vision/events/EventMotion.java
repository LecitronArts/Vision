package dev.vision.events;

import me.empty.api.event.Cancelable;

public class EventMotion extends Cancelable {
    private float yaw;
    private float pitch;
    private double x;
    private double y;
    private double z;
    private boolean ground;
    private final EventType type;

    public EventMotion(float yaw, float pitch, double x, double y, double z, boolean ground, EventType type) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ground = ground;
        this.type = type;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public boolean onGround() {
        return ground;
    }

    public void setGround(boolean ground) {
        this.ground = ground;
    }

    public void setRotation(float yaw, float pitch) {
        this.setYaw(yaw);
        this.setPitch(pitch);
    }

    public boolean isPre() {
        return this.type == EventType.Pre;
    }

    public boolean isPost() {
        return this.type == EventType.Post;
    }
}
