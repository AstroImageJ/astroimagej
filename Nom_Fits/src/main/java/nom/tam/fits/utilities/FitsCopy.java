package nom.tam.fits.utilities;

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

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.util.FitsFile;

/**
 * A simple program for copying FITS files, albeit not byte for byte. Rather, it
 * reads HDUs from the input file (first argument), interpreting it, and then
 * writing them out to the output (second argument).
 */
public final class FitsCopy {

    private FitsCopy() {
    }

    /**
     * The main class for this program
     * 
     * @param args
     *            The command-line parameters: (1) the input file name/path, and
     *            (2) the output file name/path.
     * @throws Exception
     *             Whatever exception this program might throw.
     */
    public static void main(String[] args) throws Exception {
        String file = args[0];
        try (Fits f = new Fits(file)) {
            int i = 0;
            BasicHDU<?> h;

            do {
                h = f.readHDU();
                if (h != null) {
                    if (i == 0) {
                        System.out.println("\n\nPrimary header:\n");
                    } else {
                        System.out.println("\n\nExtension " + i + ":\n");
                    }
                    i++;
                    h.info(System.out);
                }
            } while (h != null);
            f.close();
            try (FitsFile bf = new FitsFile(args[1], "rw")) {
                f.write(bf);
                bf.close();
            }
        }
    }
}
