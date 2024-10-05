package dev.vision.module.modules.screen;

import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.module.ModuleInfo;
import dev.vision.screen.ClickGuiScreen;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "ClickGUI", displayName = "Click GUI", category = Category.Screen, keyBind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGUI extends BasicModule {
    public float x;
    public float y;

    @Override
    public void onEnable() {
        mc.setScreen(new ClickGuiScreen());
        this.setEnabled(false);
    }
}
