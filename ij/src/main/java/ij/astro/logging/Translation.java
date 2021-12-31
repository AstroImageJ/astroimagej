package ij.astro.logging;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The name of the class to use for logging purposes.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Translation {
    String value();

    boolean trackThread() default false;
}
