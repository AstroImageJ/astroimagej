package nom.tam.util;

/*
 * #%L
 * nom.tam FITS library
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

import nom.tam.fits.FitsFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;

public class BufferedFileTest {

    @Before
    @After
    public void setDefaults() {
        FitsFactory.setDefaults();
    }

    @Test
    public void testCheckEof() throws IOException {
        FitsFile file = new FitsFile("target/BufferedFileCheckEof", "rw");
        file.write(new byte[2]);
        file.close();
        file = new FitsFile("target/BufferedFileCheckEof", "rw");
        try {
            // there are only 2 so ready them
            Assert.assertEquals(2, file.read(new char[3]));
            EOFException eofException = null;
            try {
                // nothing left now a eof should happen
                file.read(new char[3]);
            } catch (EOFException e) {
                eofException = e;
            }
            Assert.assertNotNull(eofException);
        } finally {
            file.close();
        }
    }

    @Test
    public void testReadWriteAscii() throws IOException {
        FitsFactory.setUseUnicodeChars(false);
        FitsFile file = new FitsFile("target/BufferedFileReadWriteAscii", "rw");
        file.write(new byte[10]);
        Assert.assertTrue(file.getChannel().isOpen());
        file.close();
        file = new FitsFile("target/BufferedFileReadWriteAscii", "rw");
        try {
            file.write(new char[2]);
            Assert.assertEquals(3, file.read(new char[3]));
        } finally {
            file.close();
        }
    }

    @Test
    public void testReadWriteUnicode() throws IOException {
        FitsFactory.setUseUnicodeChars(true);
        FitsFile file = new FitsFile("target/BufferedFileReadWriteUnicode", "rw");
        file.write(new byte[10]);
        Assert.assertTrue(file.getChannel().isOpen());
        file.close();
        file = new FitsFile("target/BufferedFileReadWriteUnicode", "rw");
        try {
            file.write(new char[2]);
            Assert.assertEquals(6, file.read(new char[3]));
        } finally {
            file.close();
        }
    }

    @Test
    public void testBigMark() throws IOException {
        FitsFile file = new FitsFile("target/BufferedFileBigMark", "rw");
        file.write(new byte[10]);
        file.close();
        file = new FitsFile("target/BufferedFileBigMark", "rw");
        Assert.assertTrue(file.markSupported());
        try {
            file.read();
            long expected = file.getFilePointer();
            file.mark(20);
            file.read();
            file.reset();
            Assert.assertEquals(expected, file.getFilePointer());
        } finally {
            file.close();
        }
    }

    @Test
    public void testReadFully() throws IOException {
        FitsFile file = new FitsFile("target/BufferedFileReadFully", "rw");
        file.write(0xffffffff);
        file.write(0xffffffff);
        file.write(0xffffffff);
        file.close();
        file = new FitsFile("target/BufferedFileReadFully", "rw");
        try {
            byte[] fully = new byte[3];
            file.readFully(fully);
            Assert.assertArrayEquals(new byte[] {-1, -1, -1}, fully);
        } finally {
            file.close();
        }
    }

    @Test
    public void testReadFully2() throws IOException {
        FitsFile file = new FitsFile("target/BufferedFileReadFully2", "rw");
        file.write(0xffffffff);
        file.write(0xffffffff);
        file.write(0xffffffff);
        file.close();
        file = new FitsFile("target/BufferedFileReadFully2", "rw");
        try {
            byte[] fully = new byte[3];
            file.readFully(fully, 0, fully.length);
            Assert.assertArrayEquals(new byte[] {-1, -1, -1}, fully);
        } finally {
            file.close();
        }
    }
}
