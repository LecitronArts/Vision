package dev.vision.interfaces;

public interface InstanceToggle {
    default void setEnabled(boolean enabled) {}

    default void toggle() {};

    default void onEnable() {}

    default void onDisable() {}
}
