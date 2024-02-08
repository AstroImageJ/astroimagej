package nom.tam.fits;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 2004 - 2024 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

/**
 * Exception that is generated when a header record is in violation of the
 * standard, or encounters some other header card specific issue.
 * 
 * @author David Glowacki
 */
public class HeaderCardException extends FitsException {

    /**
     *
     */
    private static final long serialVersionUID = 8575246321991786202L;

    /**
     * Instantiates this exception with the designated message string.
     * 
     * @param s
     *            a human readable message that describes what in fact caused
     *            the exception
     */
    public HeaderCardException(String s) {
        super(s);
    }

    /**
     * Instantiates this exception with the designated message string, when it
     * was triggered by some other type of exception
     * 
     * @param s
     *            a human readable message that describes what in fact caused
     *            the exception
     * @param reason
     *            the original exception (or other throwable) that triggered
     *            this exception.
     */
    public HeaderCardException(String s, Throwable reason) {
        super(s, reason);
    }
}
