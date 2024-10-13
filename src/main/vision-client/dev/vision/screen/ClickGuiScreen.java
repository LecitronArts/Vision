package dev.vision.screen;

import dev.vision.Vision;
import dev.vision.module.Category;
import dev.vision.module.modules.screen.ClickGUI;
import dev.vision.util.HoveringUtils;
import dev.vision.util.font.FontLoaders;
import me.empty.nanovg.NanoVGHelper;
import me.empty.nanovg.impl.draw.NanoRender2DUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private Category category = Category.Movement;

    private float x, y;
    private float moveX, moveY;

    private float mouseX;
    private float mouseY;

    private float categoryY = y + 48;

    private static final Map<Category, CategoryWindow> categoryWindowMap = new HashMap<>();

    public ClickGuiScreen() {
        super(Component.nullToEmpty("ClickGUI"));
        ClickGUI clickGUI = (ClickGUI) Vision.INSTANCE.moduleManager.getModule(ClickGUI.class);
        this.x = clickGUI.x;
        this.y = clickGUI.y;
        if (categoryWindowMap.isEmpty()) {
            for (Category category : Category.values()) {
                categoryWindowMap.put(category, new CategoryWindow(category));
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float partialTick) {
        this.mouseX = pMouseX;
        this.mouseY = pMouseY;

        NanoVGHelper.INSTANCE.begin();
        // Background-Start
        NanoRender2DUtil.drawDropShadow(x, y, 510, 374, 10, 0, 5, new Color(17, 17, 17, 80));
        NanoRender2DUtil.drawRound(x, y, 120, 337, 5, 0, 0, 0, new Color(17,17,17, 240));
        NanoRender2DUtil.drawRect(x, y + 337, 120, 1, new Color(54,54,54, 240));
        NanoRender2DUtil.drawRound(x, y + 338, 120, 36, 0, 5, 0, 0, new Color(17,17,17, 240));
        NanoRender2DUtil.drawRect(x + 120, y, 1, 374, new Color(54,54,54, 240));
        NanoRender2DUtil.drawRound(x + 121, y, 390, 44, 0, 0, 5, 0, new Color(12,12,16, 250));
        NanoRender2DUtil.drawRect(x + 121, y + 44, 390, 1, new Color(54,54,54, 240));
        NanoRender2DUtil.drawRound(x + 121, y + 45, 390, 329, 0, 0, 0, 5, new Color(10,10,10, 250));
        // Background-End

        // Title
        FontLoaders.roboto_bold_34.drawCenteredString("NEVERLOSE".toUpperCase(), x + 60, y + 16, new Color(-1));

        // Category
        float categoryY = 0;
        NanoRender2DUtil.drawRound(x + 8, this.categoryY, 104, 18, 3, new Color(255, 255, 255, 40));
        for (Category category : Category.values()) {
            if (category == this.category) {
                this.categoryY = y + 48 + categoryY;
            }
            FontLoaders.neverlose_17.drawString(category.name(), x + 20, y + 53 + categoryY, new Color(-1));
            categoryY += 22;
        }

        if (this.category != null) {
            categoryWindowMap.get(this.category).render(x + 121, y + 45, 390, 329, this.mouseX, this.mouseY);
        }

        NanoVGHelper.INSTANCE.end();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float categoryY = 0;
        for (Category category : Category.values()) {
            if (category != this.category && HoveringUtils.isHovering(x + 8, y + 48 + categoryY, 104, 18, this.mouseX, this.mouseY)) {
                this.category = category;
            }
            categoryY += 22;
        }

        if (this.category != null) {
            categoryWindowMap.get(this.category).clicked(x + 121, y + 45, 390, 329, this.mouseX, this.mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        this.mouseX = (float) pMouseX;
        this.mouseY = (float) pMouseY;

        if (HoveringUtils.isHovering(this.x, this.y, 120, 46, mouseX, mouseY) && pButton == 0) {
            if (moveX == 0 && moveY == 0) {
                moveX = mouseX - x;
                moveY = mouseY - y;
            } else {
                x = mouseX - moveX;
                y = mouseY - moveY;
            }
            //previousMouse = true;
        } else if (moveX != 0 || moveY != 0) {
            moveX = 0;
            moveY = 0;
        }

        return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    @Override
    public void onClose() {
        ClickGUI clickGUI = (ClickGUI) Vision.INSTANCE.moduleManager.getModule(ClickGUI.class);
        clickGUI.x = this.x;
        clickGUI.y = this.y;
        super.onClose();
    }
}
