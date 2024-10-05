package dev.vision.util.font;

import dev.vision.Vision;
import me.empty.nanovg.impl.font.InstanceFontRenderer;
import me.empty.nanovg.impl.font.NanoFontRenderer;

public class FontLoaders {
    public static InstanceFontRenderer neverlose_16 = getFont("neverlose.ttf", 16);
    public static InstanceFontRenderer neverlose_17 = getFont("neverlose.ttf", 17);
    public static InstanceFontRenderer neverlose_18 = getFont("neverlose.ttf", 18);

    public static InstanceFontRenderer roboto_bold_18 = getFont("roboto_bold.ttf", 18);
    public static InstanceFontRenderer roboto_bold_34 = getFont("roboto_bold.ttf", 34);
    public static InstanceFontRenderer roboto_regular_18 = getFont("roboto_regular.ttf", 18);

    public static InstanceFontRenderer pingfang_bold_18 = getFont("pingfang_bold.ttf", 18);
    public static InstanceFontRenderer source_han_sans_normal_16 = getFont("source_han_sans_normal.otf", 16);
    public static InstanceFontRenderer source_han_sans_normal_18 = getFont("source_han_sans_normal.otf", 18);

    private static NanoFontRenderer getFont(String name, float size) {
        return new NanoFontRenderer(name, "/assets/" + Vision.INSTANCE.CLIENT_NAME.toLowerCase() + "/" + name, size);
    }
}
