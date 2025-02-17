package dev.vision.module;

import dev.vision.interfaces.InstanceToggle;
import me.empty.api.event.handler.EventManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;


public class BasicModule implements InstanceToggle {
    private final ModuleInfo moduleInfo = getClass().getAnnotation(ModuleInfo.class);
    private String suffix = "";
    private int key = moduleInfo.keyBind();
    private boolean enabled = false;
    private final List<Value<?>> values = new ArrayList<>();
    public Minecraft mc = Minecraft.getInstance();

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            EventManager.register(this);
            if (mc.player != null) {
                onEnable();
            }
        } else {
            EventManager.unregister(this);
            if (mc.player != null) {
                onDisable();
            }
        }
    }

    @Override
    public void toggle() {
        setEnabled(!enabled);
    }

    public String getName() {
        return moduleInfo.name();
    }

    public String getDisplayName() {
        return moduleInfo.displayName();
    }

    public String getDescription() {
        return moduleInfo.description();
    }

    public Category getCategory() {
        return moduleInfo.category();
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(Object suffix) {
        this.suffix = String.valueOf(suffix);
    }

    public void setSuffix() {
        this.suffix = "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean enableOnStartUp() {
        return moduleInfo.enableOnStartUp();
    }

    public List<Value<?>> getValues() {
        return values;
    }
}
