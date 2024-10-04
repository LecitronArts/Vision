package me.empty.api.event.handler;

import me.empty.api.event.component.EventTarget;
import me.empty.api.event.Cancelable;

import java.lang.reflect.Method;
import java.util.*;

public class EventManager {
    private static final Map<Class<? extends Cancelable>, TreeMap<Integer, List<Listener>>> registerMap = new HashMap<>();
    private static final ArrayList<Object> registerList = new ArrayList<>();

    public static void register(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 1 && method.isAnnotationPresent(EventTarget.class)) {
                Class<? extends Cancelable> eventType = (Class<? extends Cancelable>) method.getParameterTypes()[0];
                Listener listener = new Listener(method, object, method.getAnnotation(EventTarget.class).priority().getValue());
                registerMap.computeIfAbsent(eventType, k -> new TreeMap<>()).computeIfAbsent(listener.getPriority(), k -> new ArrayList<>()).add(listener);
                registerList.add(object);
            }
        }
    }

    public static void unregister(Object object) {
        if (registerList.contains(object)) {
            for (TreeMap<Integer, List<Listener>> priorityMap : registerMap.values()) {
                for (List<Listener> listeners : priorityMap.values()) {
                    listeners.removeIf(listener -> listener.getParent() == object);
                }
            }
        }
    }

    public static void call(Cancelable event) {
        try {
            TreeMap<Integer, List<Listener>> listeners = registerMap.get(event.getClass());
            if (listeners != null) {
                for (List<Listener> listenerList : listeners.values()) {
                    for (Listener listener : listenerList) {
                        listener.getHandler().invokeExact(listener.getParent(), event);
                    }
                }
            }
        } catch (Throwable ignored) {
            new String("OH SHIT,I DON'T KNOW HOW TO FIX THIS ERROR.");
        }
    }
}
