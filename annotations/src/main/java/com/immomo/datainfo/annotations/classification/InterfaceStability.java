package com.immomo.datainfo.annotations.classification;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by kangkai on 18/2/1.
 */
/**
 * Annotation to inform users of how much to rely on a particular package,
 * class or method not changing over time.
 * </ul>
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class InterfaceStability {
    /**
     * Can evolve while retaining compatibility for minor release boundaries.;
     * can break compatibility only at major release (ie. at m.0).
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Stable {};

    /**
     * Evolving, but can break compatibility at minor release (i.e. m.x)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Evolving {};

    /**
     * No guarantee is provided as to reliability or stability across any
     * level of release granularity.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Unstable {};
}
