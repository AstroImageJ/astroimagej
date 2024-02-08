package nom.tam.util;

/*-
 * #%L
 * nom.tam.fits
 * %%
 * Copyright (C) 1996 - 2024 nom-tam-fits
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

import org.junit.Assert;
import org.junit.Test;

import java.text.ParsePosition;

@SuppressWarnings("javadoc")
public class AsciiFuncsTest {

    @Test
    public void testParseSimpleInteger() throws Exception {
        ParsePosition pos = new ParsePosition(0);
        Assert.assertEquals(-123, AsciiFuncs.parseInteger("-123", pos));
        Assert.assertEquals(4, pos.getIndex());
    }

    @Test
    public void testParseIntegerToNonDigit() throws Exception {
        ParsePosition pos = new ParsePosition(0);
        Assert.assertEquals(-123, AsciiFuncs.parseInteger("-123x", pos));
        Assert.assertEquals(4, pos.getIndex());
    }

    @Test
    public void testParseIntegerLeadingSpace() throws Exception {
        ParsePosition pos = new ParsePosition(0);
        Assert.assertEquals(-123, AsciiFuncs.parseInteger("\t -123", pos));
        Assert.assertEquals(6, pos.getIndex());
    }

}
