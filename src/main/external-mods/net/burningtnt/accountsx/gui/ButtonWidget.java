package net.burningtnt.accountsx.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ButtonWidget extends Button {
    public ButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, textSupplier -> Component.empty());
    }
}