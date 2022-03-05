package nom.tam.util;

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
            Assert.assertArrayEquals(new byte[]{
                -1,
                -1,
                -1
            }, fully);
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
            Assert.assertArrayEquals(new byte[]{
                -1,
                -1,
                -1
            }, fully);
        } finally {
            file.close();
        }
    }
}
