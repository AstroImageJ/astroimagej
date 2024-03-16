package nom.tam.image.compression.hdu;

import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.header.Compression;
import nom.tam.fits.util.BlackBoxImages;
import nom.tam.util.Cursor;
import nom.tam.util.SafeClose;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

public class CompressedTableBlackBoxTest {

    @Test
    public void testUncompress_mddtsapcln() throws Exception {
        uncompressTableAndAssert("bintable/mddtsapcln.fits.fz", "bintable/mddtsapcln.fits");
    }

    private void compressThenUncompressTableAndAssert(String originalFileName) throws FitsException, IOException {
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_GZIP_1);
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_GZIP_2);
        // compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_RICE_1);
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_NOCOMPRESS);
    }

    private void compressIntThenUncompressTableAndAssert(String originalFileName) throws FitsException, IOException {
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_GZIP_1);
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_GZIP_2);
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_RICE_1);
        compressThenUncompressTableAndAssert(originalFileName, Compression.ZCMPTYPE_NOCOMPRESS);
    }

    private void compressThenUncompressTableAndAssert(String originalFileName, String algo)
            throws FitsException, IOException {
        String tableOrgFile = BlackBoxImages.getBlackBoxImage(originalFileName);
        String[] algos = new String[100];
        Arrays.fill(algos, algo);

        File file = new File("target/" + originalFileName + ".fz");
        file.getParentFile().mkdirs();

        try (Fits fitsCompressed = new Fits(); Fits fitsOrg = new Fits(tableOrgFile)) {
            CompressedTableHDU cHDU = CompressedTableHDU.fromBinaryTableHDU((BinaryTableHDU) fitsOrg.getHDU(1), 0, algos)//
                    .compress();

            for (int col = 0; col < cHDU.getNCols(); col++) {
                Assert.assertEquals(algo, cHDU.getHeader().getStringValue(Compression.ZCTYPn.n(col + 1)));
            }

            fitsCompressed.addHDU(cHDU);

            fitsCompressed.write(file);
            fitsCompressed.close();
            fitsOrg.close();
        }

        uncompressTableAndAssert(file.getAbsolutePath(), originalFileName);
    }

    private void uncompressTableAndAssert(String compressedfileName, String originalFileName)
            throws FitsException, IOException {
        String tableFile;
        if (new File(compressedfileName).exists()) {
            tableFile = compressedfileName;
        } else {
            tableFile = BlackBoxImages.getBlackBoxImage(compressedfileName);
        }
        Fits fitsComp = null;
        String tableOrgFile = BlackBoxImages.getBlackBoxImage(originalFileName);
        Fits fitsOrg = null;
        try {
            fitsComp = new Fits(tableFile);
            fitsComp.readHDU(); // skip image
            CompressedTableHDU compressedTable = (CompressedTableHDU) fitsComp.readHDU();
            BinaryTableHDU uncompressedTable = compressedTable.asBinaryTableHDU();

            fitsOrg = new Fits(tableOrgFile);
            fitsOrg.readHDU(); // skip image
            BinaryTableHDU orgTable = compressedTable.asBinaryTableHDU();

            assertEquals(orgTable, uncompressedTable);

            fitsOrg.close();
        } finally {
            SafeClose.close(fitsComp);
            SafeClose.close(fitsOrg);
        }
    }

    private void assertEquals(BinaryTableHDU orgTable, BinaryTableHDU testTable) throws FitsException {
        int numberOfCards = orgTable.getHeader().getNumberOfCards();
        // Assert.assertEquals(numberOfCards, testTable.getHeader().getNumberOfCards());
        Cursor<String, HeaderCard> orgIterator = orgTable.getHeader().iterator();
        for (int index = 0; index < numberOfCards; index++) {
            HeaderCard orgCard = orgIterator.next();
            HeaderCard testCard = testTable.getHeader().findCard(orgCard.getKey());
            Assert.assertEquals("header " + orgCard.getKey(), orgCard.getValue(), testCard.getValue());
        }
        for (int column = 0; column < orgTable.getNCols(); column++) {
            for (int row = 0; row < orgTable.getNRows(); row++) {
                Object orgValue = orgTable.getElement(row, column);
                Object testValue = testTable.getElement(row, column);
                assertValues("col=" + column + ", row=" + row, orgValue, testValue);
            }
        }
    }

    private void assertValues(String label, Object orgValue, Object testValue) {
        if (orgValue.getClass().isArray()) {
            int arraySize = Array.getLength(orgValue);
            for (int arrayIndex = 0; arrayIndex < arraySize; arrayIndex++) {
                Object orgValueElement = Array.get(orgValue, arrayIndex);
                Object testValueElement = Array.get(testValue, arrayIndex);
                assertValues(label + ":" + arrayIndex, orgValueElement, testValueElement);
            }
        } else {
            Assert.assertEquals(orgValue, testValue);
        }
    }

    @Test
    public void testUncompress_swp06542llg() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/swp06542llg.fits.fz", "bintable/swp06542llg.fits");
    }

    @Test
    public void testUncompress_testdata() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/testdata.fits.fz", "bintable/testdata.fits");
    }

    @Test
    @Ignore // TODO also cfitsio can not uncompress this, mail to bill 22.7.2016
    public void testUncompress_tst0010() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/tst0010.fits.fz", "bintable/tst0010.fits");
    }

    @Test
    @Ignore // TODO also cfitsio can not uncompress this, mail to bill 22.7.2016
    public void testUncompress_tst0012() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/tst0012.fits.fz", "bintable/tst0012.fits");
    }

    @Test
    public void testUncompress_tst0014() throws FitsException, IOException {
        uncompressTableAndAssert("bintable/tst0014.fits.fz", "bintable/tst0014.fits");
    }

    // TODO add fpack files to blackbox images, and enable tests once fpack fixes its
    // critical bugs.
    // @Test
    // public void testUncompress_vtab_p() throws FitsException, IOException {
    // uncompressTableAndAssert("bintable/vtab.p.fits.fz", "bintable/vtab.p.fits");
    // }
    //
    // @Test
    // public void testUncompress_vtab_q() throws FitsException, IOException {
    // uncompressTableAndAssert("bintable/vtab.q.fits.fz", "bintable/vtab.q.fits");
    // }

    @Test
    public void testCompressAndUncompress_dddtsuvdata() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/dddtsuvdata.fits");
    }

    @Test
    public void testCompressAndUncompress_mddtsapcln() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/mddtsapcln.fits");
    }

    @Test
    public void testCompressAndUncompress_swp06542llg() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/swp06542llg.fits");
    }

    @Test
    public void testCompressAndUncompress_testdata() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/testdata.fits");
    }

    @Test
    public void testCompressAndUncompress_tst0010() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/tst0010.fits");
    }

    @Test
    public void testCompressAndUncompress_tst0012() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/tst0012.fits");
    }

    @Test
    public void testCompressAndUncompress_tst0014() throws FitsException, IOException {
        compressThenUncompressTableAndAssert("bintable/tst0014.fits");
    }

    @Test
    public void testCompressAndUncompress_vtab_p() throws FitsException, IOException {
        compressIntThenUncompressTableAndAssert("bintable/vtab.p.fits");
    }

    @Test
    public void testCompressAndUncompress_vtab_q() throws FitsException, IOException {
        compressIntThenUncompressTableAndAssert("bintable/vtab.q.fits");
    }

    @Test(expected = Exception.class)
    public void test_vtab_q_reversed() throws Exception {
        try {
            CompressedTableHDU.useOldStandardVLAIndexing(true);
            compressIntThenUncompressTableAndAssert("bintable/vtab.q.fits");
        } finally {
            CompressedTableHDU.useOldStandardVLAIndexing(false);
        }
    }

    @Test(expected = Exception.class)
    public void test_vtab_p_reversed() throws Exception {
        try {
            CompressedTableHDU.useOldStandardVLAIndexing(true);
            compressIntThenUncompressTableAndAssert("bintable/vtab.p.fits");
        } finally {
            CompressedTableHDU.useOldStandardVLAIndexing(false);
        }
    }
}
