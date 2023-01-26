package ij.astro.io.prefs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used by {@link Property} to override parts of the property key.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyKey {
    /**
     * @return the property key base, or the property key if {@link #ignoreAffixes()} is {@code true}.
     */
    String value();

    /**
     * @return if the {@link #value()} should replace the entirety of the generated property key.
     */
    boolean ignoreAffixes() default false;
}
