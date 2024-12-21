package dev.vision.module;

import dev.vision.events.EventKeyPress;
import me.empty.api.event.component.EventTarget;
import me.empty.api.event.handler.EventManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<BasicModule> modules = new ArrayList<>();

    public void init() {
        modules.clear();
        // Combat

        // Movement

        // HUD

        // Screen

        EventManager.register(this);
    }

    private void add(BasicModule module) {
        for (Field field : module.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object obj = field.get(module);
                if (obj instanceof Value<?>) {
                    module.getValues().add((Value) obj);
                }
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
        modules.add(module);
        if (module.enableOnStartUp()) {
            module.setEnabled(true);
        }
    }

    public List<BasicModule> getModules() {
        return modules;
    }

    public BasicModule getModule(Class<? extends BasicModule> cls) {
        return modules.stream()
                .filter(m -> m.getClass().equals(cls))
                .findFirst()
                .orElse(null);
    }

    public BasicModule getModule(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @EventTarget
    private void onKeyPress(EventKeyPress event) {
        for (BasicModule basicModule : this.getModules()) {
            if (basicModule.getKey() == event.getKey()) {
                basicModule.toggle();
            }
        }
    }
}
