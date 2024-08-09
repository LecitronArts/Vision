package net.burningtnt.accountsx.accounts.gui;

import com.mojang.blaze3d.platform.ClipboardManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

public interface Toast {
    static void copy(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) {
            new ClipboardManager().setClipboard(client.getWindow().getWindow(), text);
        } else {
            client.tell(() -> new ClipboardManager().setClipboard(client.getWindow().getWindow(), text));
        }
    }

    static void show(Type type, String title, String description, Object... args) {
        if (Minecraft.getInstance().isSameThread()) {
            SystemToast.addOrUpdate(
                    Minecraft.getInstance().getToasts(),
                    Type.MAPPING.get(type),
                    Component.translatable(title),
                    description == null ? null : Component.translatable(description, args)
            );
        } else {
            Minecraft.getInstance().tell(() -> SystemToast.add(
                    Minecraft.getInstance().getToasts(),
                    Type.MAPPING.get(type),
                    Component.translatable(title),
                    description == null ? null : Component.translatable(description, args)
            ));
        }
    }

    enum Type {
        TUTORIAL_HINT,
        NARRATOR_TOGGLE,
        WORLD_BACKUP,
        PACK_LOAD_FAILURE,
        WORLD_ACCESS_FAILURE,
        PACK_COPY_FAILURE,
        PERIODIC_NOTIFICATION,
        UNSECURE_SERVER_WARNING;

        private static final EnumMap<Type, SystemToast.SystemToastId> MAPPING = new EnumMap<>(Type.class);

        static {
            MAPPING.putAll(Map.of(
                    TUTORIAL_HINT, SystemToast.SystemToastId.NARRATOR_TOGGLE,
                    NARRATOR_TOGGLE, SystemToast.SystemToastId.NARRATOR_TOGGLE,
                    WORLD_BACKUP, SystemToast.SystemToastId.WORLD_BACKUP,
                    PACK_LOAD_FAILURE, SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                    WORLD_ACCESS_FAILURE, SystemToast.SystemToastId.PACK_LOAD_FAILURE,
                    PACK_COPY_FAILURE, SystemToast.SystemToastId.PACK_COPY_FAILURE,
                    PERIODIC_NOTIFICATION, SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    UNSECURE_SERVER_WARNING, SystemToast.SystemToastId.UNSECURE_SERVER_WARNING
            ));
        }
    }
}
