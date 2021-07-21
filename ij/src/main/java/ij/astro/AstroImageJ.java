package ij.astro;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation marking code within ImageJ as originating or having been modified for the purposes of
 * AstroImageJ.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface AstroImageJ {
    /**
     * @return the reason for this modification.
     */
    String reason();

    /**
     * @return the AIJ version that originates this change.
     */
    String since() default "5.0.0";

    /**
     * @return the author of this change.
     */
    String author() default "";

    /**
     * @return whether the annotated code existed within ImageJ and was modified for use with AIJ
     */
    boolean modified() default false;

    /**
     * @return if the usage or implementation of this feature was lost.
     */
    boolean lostImplementation() default false;


    /**
     * @return if the usage of this annotated member was lost.
     */
    boolean unused() default false;
}
