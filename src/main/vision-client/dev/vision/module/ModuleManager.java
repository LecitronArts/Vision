package dev.vision.module;

import dev.vision.events.EventKeyPress;
import dev.vision.module.modules.movement.Sprint;
import me.empty.api.event.component.EventTarget;
import me.empty.api.event.handler.EventManager;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<BasicModule> modules = new ArrayList<>();

    public void init() {
        modules.clear();
        add(new Sprint());

        EventManager.register(this);
    }

    private void add(BasicModule module) {
        modules.add(module);
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
