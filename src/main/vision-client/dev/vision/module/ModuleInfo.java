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
    String name();

    String displayName();

    String description() default "";

    Category category();

    int keyBind() default 0;

    boolean enableOnStartUp() default false;
}