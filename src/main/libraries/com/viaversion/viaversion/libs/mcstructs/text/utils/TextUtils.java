package com.viaversion.viaversion.libs.mcstructs.text.utils;

import com.viaversion.viaversion.libs.mcstructs.core.TextFormatting;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.KeybindComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.EntityHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtils {

    private static final String URL_PATTERN = "(?:https?://)?[\\w._-]+\\.\\w{2,}(?:/\\S*)?";

    /**
     * Make URLs in the given text component clickable.
     *
     * @param component The component to make clickable
     * @return The component with clickable URLs
     */
    public static ATextComponent makeURLsClickable(final ATextComponent component) {
        return replace(component, URL_PATTERN, comp -> {
            comp.getStyle().setClickEvent(new ClickEvent(ClickEventAction.OPEN_URL, comp.asSingleString()));
            return comp;
        });
    }

    /**
     * Replace components in the given component and all siblings with the given function.<br>
     * This includes all arguments of translation components.
     *
     * @param component       The component to replace in
     * @param replaceFunction The function that will be called for every component
     * @return A new component with the replaced components
     */
    public static ATextComponent replace(final ATextComponent component, final Function<ATextComponent, ATextComponent> replaceFunction) {
        ATextComponent out = component.copy();
        out.getSiblings().clear();
        out = replaceFunction.apply(out);
        if (out instanceof TranslationComponent) {
            Object[] args = ((TranslationComponent) out).getArgs();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ATextComponent) {
                    args[i] = replace((ATextComponent) args[i], replaceFunction);
                }
            }
        }
        for (ATextComponent sibling : component.getSiblings()) out.append(replace(sibling, replaceFunction));
        return out;
    }

    /**
     * Replace all matches of the given pattern in this component and all siblings.<br>
     * This only works for {@link StringComponent} components.
     *
     * @param component       The component to replace in
     * @param searchRegex     The regex to search for
     * @param replaceFunction The function that will be called for every match
     * @return A new component with the replaced text
     */
    public static ATextComponent replace(final ATextComponent component, final String searchRegex, final Function<ATextComponent, ATextComponent> replaceFunction) {
        ATextComponent out;
        Pattern pattern = Pattern.compile(searchRegex);
        if (component instanceof StringComponent) {
            String text = component.asSingleString();
            Matcher matcher = pattern.matcher(text);
            List<ATextComponent> parts = new ArrayList<>();
            int last = 0;
            while (matcher.find()) {
                int start = matcher.start();
                String match = matcher.group();

                if (start > last) parts.add(new StringComponent(text.substring(last, start)).setStyle(component.getStyle().copy()));
                ATextComponent replace = replaceFunction.apply(new StringComponent(match).setStyle(component.getStyle().copy()));
                if (replace != null) parts.add(replace);
                last = matcher.end();
            }
            if (last < text.length()) parts.add(new StringComponent(text.substring(last)).setStyle(component.getStyle().copy()));
            if (parts.size() > 1) {
                out = new StringComponent("");
                for (ATextComponent part : parts) out.append(part);
            } else {
                if (parts.size() == 1) out = parts.get(0).copy();
                else out = component.copy();
                out.getSiblings().clear();
            }
        } else {
            out = component.copy();
            out.getSiblings().clear();
        }
        for (ATextComponent sibling : component.getSiblings()) {
            ATextComponent replace = replace(sibling, searchRegex, replaceFunction);
            out.append(replace);
        }

        return out;
    }

    /**
     * Replace all rgb color codes with the nearest formatting color.<br>
     * This <b>can not</b> perfectly convert the colors since there is only a limited amount of formatting colors.<br>
     * Minecraft 1.16 has added support for rgb colors which older versions can't display.
     *
     * @param component The component to replace in
     * @return A new component with the replaced text
     */
    public static ATextComponent replaceRGBColors(final ATextComponent component) {
        ATextComponent out = component.copy();
        out.forEach(comp -> {
            if (comp.getStyle().getColor() != null && comp.getStyle().getColor().isRGBColor()) {
                comp.getStyle().setFormatting(TextFormatting.getClosestFormattingColor(comp.getStyle().getColor().getRgbValue()));
            }
        });
        return out;
    }

    /**
     * Join the given components with the given separator.<br>
     * All components are copied before they are joined.<br>
     * If there are no components an empty {@link StringComponent} will be returned.<br>
     * If there is only one component it will be copied and returned.
     *
     * @param separator  The separator
     * @param components The components
     * @return The joined component
     */
    public static ATextComponent join(final ATextComponent separator, final ATextComponent... components) {
        if (components.length == 0) return new StringComponent("");
        if (components.length == 1) return components[0].copy();

        ATextComponent out = null;
        for (ATextComponent component : components) {
            if (out == null) out = new StringComponent("").append(component.copy());
            else out.append(separator.copy()).append(component.copy());
        }
        return out;
    }

    /**
     * Iterate over all siblings of the given component.<br>
     * This includes all arguments of translation components.
     *
     * @param component The component to iterate over
     * @param consumer  The consumer that will be called for every component
     * @see ATextComponent#forEach(Consumer)
     */
    public static void iterateAll(final ATextComponent component, final Consumer<ATextComponent> consumer) {
        consumer.accept(component);
        if (component.getStyle().getHoverEvent() != null) {
            AHoverEvent hoverEvent = component.getStyle().getHoverEvent();
            if (hoverEvent instanceof TextHoverEvent) {
                iterateAll(((TextHoverEvent) hoverEvent).getText(), consumer);
            } else if (hoverEvent instanceof EntityHoverEvent) {
                ATextComponent name = ((EntityHoverEvent) hoverEvent).getName();
                if (name != null) iterateAll(name, consumer);
            }
        }
        for (ATextComponent sibling : component.getSiblings()) {
            iterateAll(sibling, consumer);
            if (sibling instanceof TranslationComponent translationComponent) {
                for (Object arg : translationComponent.getArgs()) {
                    if (arg instanceof ATextComponent) iterateAll((ATextComponent) arg, consumer);
                }
            }
        }
    }

    /**
     * Recursively set the translator for all translation and keybind components in the given component.<br>
     * This includes all arguments of translation components.
     *
     * @param component  The component to set the translator for
     * @param translator The translator function
     */
    public static void setTranslator(final ATextComponent component, @Nullable final Function<String, String> translator) {
        setTranslator(component, translator, translator);
    }

    /**
     * Recursively set the translators for all translation and keybind components in the given component.<br>
     * This includes all arguments of translation components.
     *
     * @param component      The component to set the translators for
     * @param textTranslator The translator function for text components
     * @param keyTranslator  The translator function for keybind components
     */
    public static void setTranslator(final ATextComponent component, @Nullable final Function<String, String> textTranslator, @Nullable final Function<String, String> keyTranslator) {
        iterateAll(component, comp -> {
            if (comp instanceof TranslationComponent translationComponent) {
                translationComponent.setTranslator(textTranslator);
            } else if (comp instanceof KeybindComponent keybindComponent) {
                keybindComponent.setTranslator(keyTranslator);
            }
        });
    }

}
