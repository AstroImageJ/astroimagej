package nom.tam.util;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class FitsFileTest {

    @After
    public void cleanup() {
        new File("fftest.bin").delete();
    }
    
    @Test(expected = IOException.class)
    public void testWriteNotArray() throws Exception {
        try (FitsFile f = new FitsFile("fftest.bin", "rw", 100)) {
            // Not an array
            f.writeArray("hello");
        }
    }
   
    @Test
    public void testReadWriteBooleanObjectArray() throws Exception {
        try (FitsFile f = new FitsFile("fftest.bin", "rw", 100)) {
            Boolean[] b = new Boolean[] { Boolean.TRUE, null, Boolean.FALSE };
            f.write(b);
            f.seek(0);
            Boolean[] b2 = new Boolean[b.length];
            f.read(b2);
            f.close();
            for (int i=0; i<b.length; i++) {
                assertEquals("[" + i + "]", b[i], b2[i]);
            }
        }
    }
    
    @Test(expected = IOException.class)
    public void testSkipBeforeBeginning() throws Exception {
        try (FitsFile f = new FitsFile("fftest.bin", "rw", 100)) {
            f.seek(10);
            f.skipAllBytes(-11L);
        }
    }
    
    
    @Test
    public void testPosition() throws Exception {
        try (FitsFile f = new FitsFile("fftest.bin", "rw", 100)) {
            f.position(10);
            assertEquals(10, f.position());
        }
    }

    @Test
    public void testAltRandomAccess() throws Exception {
        try (final FitsFile f = new FitsFile(new BufferedFileIO.RandomFileIO(
                new File("fftest.bin"), "rw"), 1024)) {
            f.seek(12L);
            assertEquals("Wrong position", 12L, f.position());
        }
    }
}
