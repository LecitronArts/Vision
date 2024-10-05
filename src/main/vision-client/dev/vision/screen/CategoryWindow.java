package dev.vision.screen;

import dev.vision.Vision;
import dev.vision.module.BasicModule;
import dev.vision.module.Category;
import dev.vision.util.HoveringUtils;
import dev.vision.util.font.FontLoaders;
import me.empty.nanovg.impl.draw.NanoRender2DUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryWindow {
    public Category category;

    public CategoryWindow(Category category) {
        this.category = category;
    }

    public void render(float x, float y, float maxWidth, float maxHeight, float mouseX, float mouseY) {
        List<BasicModule> modules = new ArrayList<>();
        for (BasicModule module : Vision.INSTANCE.moduleManager.getModules()) {
            if (module.getCategory() == this.category) {
                modules.add(module);
            }
        }

        if (modules.isEmpty()) {
            return;
        }

        float moduleWindowWidth = (maxWidth - 16) / 2 - 8;

        int moduleIndex = 0;
        float rightModuleY = 0;
        float leftModuleY = 0;
        for (BasicModule module : modules) {
            String keyName = GLFW.glfwGetKeyName(module.getKey(), -1) == null ? "" : " [" + GLFW.glfwGetKeyName(module.getKey(), -1) + "]";
            String name = module.getName() + keyName;
            if (moduleIndex > moduleIndex / 2) {
                NanoRender2DUtil.drawRound(x + 16 + moduleWindowWidth, y + 18 + rightModuleY, moduleWindowWidth, 24, 4, new Color(63,63,63));
                NanoRender2DUtil.drawRound(x + 16.5F + moduleWindowWidth, y + 18.5F + rightModuleY, moduleWindowWidth - 1, 23, 4, new Color(13,13,13));
                FontLoaders.neverlose_16.drawString(name, x + 22 + moduleWindowWidth, y + 8, new Color(69,69,69));

                FontLoaders.pingfang_bold_18.drawString("Enabled", x + 24 + moduleWindowWidth, y + rightModuleY + 26, new Color(-1));
                NanoRender2DUtil.drawRound(x + moduleWindowWidth * 2 - 16, y + 23 + rightModuleY, 22, 14, 6, new Color(34,34,36));
                if (module.isEnabled()) {
                    NanoRender2DUtil.drawCircle(x + moduleWindowWidth * 2 - 2, y + 30 + rightModuleY, 5, new Color(-1));
                } else {
                    NanoRender2DUtil.drawCircle(x + moduleWindowWidth * 2 - 8, y + 30 + rightModuleY, 5, new Color(55,55,55));
                }
                rightModuleY += 22;
            } else {
                NanoRender2DUtil.drawRound(x + 8, y + 18 + leftModuleY, moduleWindowWidth, 24, 4, new Color(63,63,63));
                NanoRender2DUtil.drawRound(x + 8.5F, y + 18.5F + leftModuleY, moduleWindowWidth - 1, 23, 4, new Color(13,13,13));
                FontLoaders.neverlose_16.drawString(name, x + 14, y + 8, new Color(69,69,69));

                FontLoaders.pingfang_bold_18.drawString("Enabled", x + 16, y + leftModuleY + 26, new Color(-1));
                NanoRender2DUtil.drawRound(x + moduleWindowWidth - 24, y + 23 + leftModuleY, 22, 14, 6.5F, new Color(34,34,36));
                if (module.isEnabled()) {
                    NanoRender2DUtil.drawCircle(x + moduleWindowWidth - 10, y + 30 + leftModuleY, 5, new Color(-1));
                } else {
                    NanoRender2DUtil.drawCircle(x + moduleWindowWidth - 16, y + 30 + leftModuleY, 5, new Color(55,55,55));
                }
                leftModuleY += 22;
            }
            moduleIndex += 1;
        }
    }

    public void clicked(float x, float y, float maxWidth, float maxHeight, float mouseX, float mouseY, int button) {
        List<BasicModule> modules = new ArrayList<>();
        for (BasicModule module : Vision.INSTANCE.moduleManager.getModules()) {
            if (module.getCategory() == this.category) {
                modules.add(module);
            }
        }

        if (modules.isEmpty()) {
            return;
        }

        float moduleWindowWidth = (maxWidth - 16) / 2 - 8;

        int moduleIndex = 0;
        float rightModuleY = 0;
        float leftModuleY = 0;
        for (BasicModule module : modules) {
            if (moduleIndex > moduleIndex / 2) {
                if (HoveringUtils.isHovering(x + moduleWindowWidth * 2 - 16, y + 23 + rightModuleY, 22, 14, mouseX, mouseY) && button == 0) {
                    module.toggle();
                }
                rightModuleY += 22;
            } else {
                if (HoveringUtils.isHovering(x + moduleWindowWidth - 24, y + 23 + leftModuleY, 22, 14, mouseX, mouseY) && button == 0) {
                    module.toggle();
                }
                leftModuleY += 22;
            }
            moduleIndex += 1;
        }
    }
}
