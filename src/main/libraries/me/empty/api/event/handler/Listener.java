package me.empty.api.event.handler;

import me.empty.api.event.Cancelable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class Listener {
    private MethodHandle handler;
    private final Object parent;
    private final int priority;

    public Listener(Method method, Object parent, int priority) {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        MethodHandle m;
        try {
            m = MethodHandles.lookup().unreflect(method);
            if (m != null) {
                this.handler = m.asType(m.type().changeParameterType(0, Object.class).changeParameterType(1, Cancelable.class));
            } else {
                throw new IllegalAccessException("MethodHandle cannot be null");
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        this.parent = parent;
        this.priority = priority;
    }

    
    public MethodHandle getHandler() {
        return handler;
    }

    
    public Object getParent() {
        return parent;
    }

    
    public int getPriority() {
        return priority;
    }
}
