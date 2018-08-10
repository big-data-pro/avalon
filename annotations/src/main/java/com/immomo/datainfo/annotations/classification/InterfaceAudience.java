package com.immomo.datainfo.annotations.classification;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by kangkai on 18/2/1.
 */
/**
 * Annotation to inform users of a package, class or method's intended audience.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class InterfaceAudience {
    /**
     * Intended for use by any project or application.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Public {};

    /**
     * Intended only for the project(s) specified in the annotation.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LimitedPrivate {
        String[] value();
    };

    /**
     * Intended for use only within Avalon itself.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Private {};

    private InterfaceAudience() {} // Audience can't exist on its own
}
