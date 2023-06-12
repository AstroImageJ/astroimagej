package nom.tam.image.compression.hdu;

import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Compression;
import nom.tam.fits.header.GenericKey;
import nom.tam.fits.header.IFitsHeader;
import nom.tam.fits.header.IFitsHeader.VALUE;
import nom.tam.util.Cursor;

import java.util.Map;

import static nom.tam.fits.header.Checksum.CHECKSUM;
import static nom.tam.fits.header.Checksum.DATASUM;
import static nom.tam.fits.header.Compression.*;
import static nom.tam.fits.header.Standard.*;

/**
 * Mapping of header keywords between compressed and uncompressed representation. For example, the keyword NAXIS1 in the
 * uncompressed HDU is remapped to ZNAXIS1 in the compressed HDU so it does not interfere with the different layout of
 * the compressed HDU vs the layout of the original one.
 */
enum BackupRestoreUnCompressedHeaderCard {
    MAP_ANY(null) {

        @Override
        protected void backupCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            // unhandled card so just copy it to the uncompressed header
            headerIterator.add(card.copy());
        }

        @Override
        protected void restoreCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            // unhandled card so just copy it to the uncompressed header
            headerIterator.add(card.copy());
        }
    },
    MAP_BITPIX(BITPIX), MAP_CHECKSUM(CHECKSUM), MAP_DATASUM(DATASUM), MAP_EXTNAME(EXTNAME) {

        @Override
        protected void backupCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            if (!card.getValue().equals("COMPRESSED_IMAGE")) {
                super.backupCard(card, headerIterator);
            }
        }
    },
    MAP_GCOUNT(GCOUNT), MAP_NAXIS(NAXIS), MAP_NAXISn(NAXISn), MAP_PCOUNT(PCOUNT),
    // MAP_TFIELDS(TFIELDS),
    MAP_ZFORMn(ZFORMn) {

        @Override
        protected void backupCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            String newKey = uncompressedHeaderKey().n(GenericKey.getN(card.getKey())).key();
            headerIterator.add(new HeaderCard(newKey, card.getValue(String.class, ""), card.getComment()));
        }

        @Override
        protected void restoreCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            String newKey = compressedHeaderKey().n(GenericKey.getN(card.getKey())).key();
            headerIterator.add(new HeaderCard(newKey, card.getValue(String.class, ""), card.getComment()));
        }

    },
    MAP_TFORMn(TFORMn), MAP_XTENSION(XTENSION), MAP_ZBITPIX(ZBITPIX), MAP_ZBLANK(ZBLANK), MAP_ZTILELEN(
            ZTILELEN), MAP_ZCTYPn(ZCTYPn), @SuppressWarnings("deprecation")
    MAP_ZBLOCKED(ZBLOCKED), MAP_ZCMPTYPE(ZCMPTYPE), MAP_ZDATASUM(ZDATASUM), MAP_ZDITHER0(ZDITHER0), MAP_ZEXTEND(
            ZEXTEND), MAP_ZGCOUNT(ZGCOUNT), MAP_ZHECKSUM(ZHECKSUM), MAP_ZIMAGE(
                    ZIMAGE), MAP_ZTABLE(ZTABLE), MAP_ZNAMEn(ZNAMEn), MAP_ZNAXIS(ZNAXIS), MAP_THEAP(THEAP) {

                        @Override
                        protected void backupCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator)
                                throws HeaderCardException {
                        }

                        @Override
                        protected void restoreCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator)
                                throws HeaderCardException {
                        }
                    },
    MAP_ZNAXISn(ZNAXISn) {

        @Override
        protected void backupCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            String newKey = uncompressedHeaderKey().n(GenericKey.getN(card.getKey())).key();
            headerIterator.add(new HeaderCard(newKey, card.getValue(Integer.class, 0), card.getComment()));
        }

        @Override
        protected void restoreCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
            String newKey = compressedHeaderKey().n(GenericKey.getN(card.getKey())).key();
            headerIterator.add(new HeaderCard(newKey, card.getValue(Integer.class, 0), card.getComment()));
        }

    },
    MAP_ZPCOUNT(ZPCOUNT), MAP_ZQUANTIZ(ZQUANTIZ), MAP_ZSIMPLE(ZSIMPLE), MAP_ZTENSION(ZTENSION), MAP_ZTILEn(
            ZTILEn), MAP_ZVALn(ZVALn);

    private final IFitsHeader compressedHeaderKey;

    private final IFitsHeader uncompressedHeaderKey;

    public static void backup(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
        BackupRestoreUnCompressedHeaderCard mapping = selectMapping(CompressedImageHDU.UNCOMPRESSED_HEADER_MAPPING, card);
        mapping.backupCard(card, headerIterator);
    }

    public static void restore(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
        BackupRestoreUnCompressedHeaderCard mapping = selectMapping(CompressedImageHDU.COMPRESSED_HEADER_MAPPING, card);
        mapping.restoreCard(card, headerIterator);
    }

    protected static BackupRestoreUnCompressedHeaderCard selectMapping(
            Map<IFitsHeader, BackupRestoreUnCompressedHeaderCard> mappings, HeaderCard card) {
        IFitsHeader key = GenericKey.lookup(card.getKey());
        if (key != null) {
            BackupRestoreUnCompressedHeaderCard mapping = mappings.get(key);
            if (mapping != null) {
                return mapping;
            }
        }
        return MAP_ANY;
    }

    BackupRestoreUnCompressedHeaderCard(IFitsHeader header) {
        compressedHeaderKey = header;
        if (header instanceof Compression) {
            uncompressedHeaderKey = ((Compression) compressedHeaderKey).getUncompressedKey();

        } else {
            uncompressedHeaderKey = null;
        }
        CompressedImageHDU.UNCOMPRESSED_HEADER_MAPPING.put(header, this);
        if (uncompressedHeaderKey != null) {
            CompressedImageHDU.COMPRESSED_HEADER_MAPPING.put(uncompressedHeaderKey, this);
        }
    }

    private void addHeaderCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator, IFitsHeader targetKey)
            throws HeaderCardException {
        if (targetKey != null) {
            if (targetKey.valueType() == VALUE.INTEGER) {
                headerIterator.add(new HeaderCard(targetKey.key(), card.getValue(Integer.class, 0), card.getComment()));
            } else if (targetKey.valueType() == VALUE.STRING) {
                headerIterator.add(new HeaderCard(targetKey.key(), card.getValue(), card.getComment()));
            } else if (targetKey.valueType() == VALUE.LOGICAL) {
                headerIterator.add(new HeaderCard(targetKey.key(), card.getValue(Boolean.class, false), card.getComment()));
            }
        }
    }

    /**
     * default behaviour is to ignore the card and by that to exclude it from the uncompressed header if it does not
     * have a uncompressed equivalent..
     *
     * @param  card                the card from the compressed header
     * @param  headerIterator      the iterator for the uncompressed header.
     *
     * @throws HeaderCardException if the card could not be copied
     */
    protected void backupCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
        IFitsHeader uncompressedKey = uncompressedHeaderKey;
        addHeaderCard(card, headerIterator, uncompressedKey);
    }

    protected IFitsHeader compressedHeaderKey() {
        return compressedHeaderKey;
    }

    protected void restoreCard(HeaderCard card, Cursor<String, HeaderCard> headerIterator) throws HeaderCardException {
        addHeaderCard(card, headerIterator, compressedHeaderKey);
    }

    protected IFitsHeader uncompressedHeaderKey() {
        return uncompressedHeaderKey;
    }
}
