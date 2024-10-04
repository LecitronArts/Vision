package dev.vision.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Annotation used to provide metadata about a module.
 * <p>
 * This annotation is applied to classes that represent modules, providing information such as the module's name, display name, description, category, key bind, and pattern.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleInfo {

    /**
     * The unique name of the module.
     * <p>
     * This name should be used to uniquely identify the module within the system.
     *
     * @return The unique name of the module.
     */
    String name();

    /**
     * The display name of the module.
     * <p>
     * This name is intended for human consumption and may be more descriptive or user-friendly than the unique name.
     *
     * @return The display name of the module.
     */
    String displayName();

    /**
     * A brief description of the module's functionality.
     * <p>
     * This description provides additional information about what the module does and how it can be used.
     *
     * @return A brief description of the module, or an empty string if not provided.
     */
    String description() default "";

    /**
     * The category that the module belongs to.
     * <p>
     * This helps to group related modules together for organizational purposes.
     *
     * @return The category that the module belongs to.
     */
    Category category();

    /**
     * The key bind associated with the module, if any.
     * <p>
     * This value can be used to assign a keyboard shortcut or other input method to activate the module.
     *
     * @return The key bind value, or 0 if not specified.
     */
    int keyBind() default 0;
}