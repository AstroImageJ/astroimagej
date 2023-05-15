package ij.astro.util;

import ij.process.*;

import java.util.function.BiFunction;

public enum ImageType {
    BYTE(ByteProcessor::new, byte[][].class) {
        @Override
        public Object processImageData(Object rawData, int width, int height, double bzero, double bscale) {
            if (rawData instanceof byte[][] values) {
                final var pixelArray = new byte[width * height];
                if (bscale == 1 && bzero == 0) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(values[height - 1 - y], 0, pixelArray, y * width, width);
                    }

                    return pixelArray;
                }

                for (int y = 0; y < height; y++) {
                    // We extract the x-values from the y-array early to avoid extra array accesses in the inner loop
                    var xValues = values[height - 1 - y];
                    for (int x = 0; x < width; x++) {
                        // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
                        if (USE_FMA) {
                            pixelArray[(y * width) + x] = (byte) Math.fma(bscale, Byte.toUnsignedInt(xValues[x]), bzero);
                        } else {
                            pixelArray[(y * width) + x] = (byte) (bzero + bscale * Byte.toUnsignedInt(xValues[x]));
                        }
                    }
                }

                return pixelArray;
            }

            throw new IllegalStateException("Incorrect raw data given to make an ImageProcessor");
        }

        @Override
        public Object make2DArray(ImageProcessor ip, boolean useBZero) {
            var lip = ((ByteProcessor) ip);

            var width = ip.getWidth();
            var height = ip.getHeight();
            // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
            var outArray = new byte[height][width];
            var values = (byte[]) lip.getPixels();
            for (int y = 0; y < height; y++) {
                System.arraycopy(values, y * width, outArray[height - 1 - y], 0, width);
            }

            return outArray;
        }

        @Override
        public double getBZero() {
            return -(double)Byte.MIN_VALUE; // Not really needed as java bytes are already unsigned
        }

        @Override
        public int getExpectedBitpix() {
            return 8;
        }
    },
    SHORT(ShortProcessor::new, short[][].class) {
        @Override
        public Object processImageData(Object rawData, int width, int height, double bzero, double bscale) {
            if (rawData instanceof short[][] values) {
                final var pixelArray = new short[width * height];
                if (bscale == 1 && bzero == 0) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(values[height - 1 - y], 0, pixelArray, y * width, width);
                    }

                    return pixelArray;
                }

                for (int y = 0; y < height; y++) {
                    // We extract the x-values from the y-array early to avoid extra array accesses in the inner loop
                    var xValues = values[height - 1 - y];
                    for (int x = 0; x < width; x++) {
                        // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
                        if (USE_FMA) {
                            pixelArray[(y * width) + x] = (short) Math.fma(bscale, xValues[x], bzero);
                        } else {
                            pixelArray[(y * width) + x] = (short) (bzero + bscale * xValues[x]);
                        }
                    }
                }

                return pixelArray;
            }

            throw new IllegalStateException("Incorrect raw data given to make an ImageProcessor");
        }

        @Override
        public Object make2DArray(ImageProcessor ip, boolean useBZero) {//todo support 3d images? (write entire stack as one layered image, with option to disable?)
            var lip = ((ShortProcessor) ip);

            var width = lip.getWidth();
            var height = lip.getHeight();
            // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
            var outArray = new short[height][width];
            if (useBZero) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        outArray[height - 1 - y][x] = (short) (lip.get(x, y) + Short.MIN_VALUE); // Subtract BZERO
                    }
                }
            } else {
                var values = (short[]) lip.getPixels();
                for (int y = 0; y < height; y++) {
                    System.arraycopy(values, y * width, outArray[height - 1 - y], 0, width);
                }
            }

            return outArray;
        }

        @Override
        public double getBZero() {
            return -(double)Short.MIN_VALUE;
        }

        @Override
        public int getExpectedBitpix() {
            return 16;
        }
    },
    INT(FloatProcessor::new/*IntProcessor::new*/, int[][].class) {//todo when using int processor, fits_reader displays invalid values
        @Override
        public Object processImageData(Object rawData, int width, int height, double bzero, double bscale) {
            if (rawData instanceof int[][] values) {
                final var pixelArray = new float[width * height];//new int[]
                //todo reenable when IntProcessor output functions
                /*if (bscale == 1 && bzero == 0) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(values[height - 1 - y], 0, pixelArray, y * width, width);
                    }

                    return pixelArray;
                }*/

                for (int y = 0; y < height; y++) {
                    // We extract the x-values from the y-array early to avoid extra array accesses in the inner loop
                    var xValues = values[height - 1 - y];
                    for (int x = 0; x < width; x++) {
                        // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
                        if (USE_FMA) {
                            pixelArray[(y * width) + x] = (float) Math.fma(bscale, xValues[x], bzero);
                        } else {
                            pixelArray[(y * width) + x] = (float) (bzero + bscale * xValues[x]);
                        }
                    }
                }

                return pixelArray;
            }

            throw new IllegalStateException("Incorrect raw data given to make an ImageProcessor");
        }

        @Override
        public Object make2DArray(ImageProcessor ip, boolean useBZero) {
            var lip = ((IntProcessor) ip);

            var width = ip.getWidth();
            var height = ip.getHeight();
            // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
            var outArray = new int[height][width];
            if (useBZero) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        outArray[height - 1 - y][x] = lip.get(x, y) + Integer.MIN_VALUE; // Subtract BZERO
                    }
                }
            } else {
                var values = (int[]) lip.getPixels();
                for (int y = 0; y < height; y++) {
                    System.arraycopy(values, y * width, outArray[height - 1 - y], 0, width);
                }
            }

            return outArray;
        }

        @Override
        public double getBZero() {
            return -(double)Integer.MIN_VALUE;
        }

        @Override
        public int getExpectedBitpix() {
            return 32;
        }
    },
    //this loses accuracy for some values
    LONG(FloatProcessor::new, long[][].class) {
        @Override
        public Object processImageData(Object rawData, int width, int height, double bzero, double bscale) {
            if (rawData instanceof long[][] values) {
                final var pixelArray = new float[width * height];
                //Disabled as we have no LongProcessor
                /*if (bscale == 1 && bzero == 0) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(values[height - 1 - y], 0, pixelArray, y * width, width);
                    }

                    return pixelArray;
                }*/

                for (int y = 0; y < height; y++) {
                    // We extract the x-values from the y-array early to avoid extra array accesses in the inner loop
                    var xValues = values[height - 1 - y];
                    for (int x = 0; x < width; x++) {
                        // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
                        if (USE_FMA) {
                            pixelArray[(y * width) + x] = (float) Math.fma(bscale, xValues[x], bzero);
                        } else {
                            pixelArray[(y * width) + x] = (float) (bzero + bscale * xValues[x]);
                        }
                    }
                }

                return pixelArray;
            }

            throw new IllegalStateException("Incorrect raw data given to make an ImageProcessor");
        }

        @Override
        public Object make2DArray(ImageProcessor ip, boolean useBZero) {
            var lip = ((FloatProcessor) ip);

            var width = ip.getWidth();
            var height = ip.getHeight();
            // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
            var outArray = new long[height][width];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    outArray[height - 1 - y][x] = lip.get(x, y) + (useBZero ? Long.MIN_VALUE : 0); // Subtract BZERO
                }
            }

            return outArray;
        }

        @Override
        public double getBZero() {
            return -(double)Long.MIN_VALUE;
        }

        @Override
        public int getExpectedBitpix() {
            return 64;
        }
    },
    FLOAT(FloatProcessor::new, float[][].class) {
        @Override
        public Object processImageData(Object rawData, int width, int height, double bzero, double bscale) {
            if (rawData instanceof float[][] values) {
                final var pixelArray = new float[width * height];

                if (bscale == 1 && bzero == 0) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(values[height - 1 - y], 0, pixelArray, y * width, width);
                    }

                    return pixelArray;
                }

                for (int y = 0; y < height; y++) {
                    // We extract the x-values from the y-array early to avoid extra array accesses in the inner loop
                    var xValues = values[height - 1 - y];
                    for (int x = 0; x < width; x++) {
                        // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
                        if (USE_FMA) {
                            pixelArray[(y * width) + x] = (float) Math.fma(bscale, xValues[x], bzero);
                        } else {
                            pixelArray[(y * width) + x] = (float) (bzero + bscale * xValues[x]);
                        }
                    }
                }

                return pixelArray;
            }

            throw new IllegalStateException("Incorrect raw data given to make an ImageProcessor");
        }

        @Override
        public Object make2DArray(ImageProcessor ip, boolean useBZero) {
            var lip = ((FloatProcessor) ip);

            var width = ip.getWidth();
            var height = ip.getHeight();
            // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
            var outArray = new float[height][width];
            if (useBZero) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        outArray[height - 1 - y][x] = lip.getf(x, y);
                    }
                }
            } else {
                var values = (float[]) lip.getPixels();
                for (int y = 0; y < height; y++) {
                    System.arraycopy(values, y * width, outArray[height - 1 - y], 0, width);
                }
            }
            return outArray;
        }

        @Override
        public boolean isFloatingPoint() {
            return true;
        }

        @Override
        public int getExpectedBitpix() {
            return -32;
        }
    },
    //this loses accuracy for some values
    DOUBLE(FloatProcessor::new, double[][].class) {
        @Override
        public Object processImageData(Object rawData, int width, int height, double bzero, double bscale) {
            if (rawData instanceof double[][] values) {
                final var pixelArray = new float[width * height];//todo make double when we have a DoubleProcessor
                //Disabled as we have no DoubleProcessor
                /*if (bscale == 1 && bzero == 0) {
                    for (int y = 0; y < height; y++) {
                        System.arraycopy(values[height - 1 - y], 0, pixelArray, y * width, width);
                    }

                    return pixelArray;
                }*/

                for (int y = 0; y < height; y++) {
                    // We extract the x-values from the y-array early to avoid extra array accesses in the inner loop
                    var xValues = values[height - 1 - y];
                    for (int x = 0; x < width; x++) {
                        // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
                        if (USE_FMA) {
                            pixelArray[(y * width) + x] = (float) Math.fma(bscale, xValues[x], bzero);
                        } else {
                            pixelArray[(y * width) + x] = (float) (bzero + bscale * xValues[x]);
                        }
                    }
                }

                return pixelArray;
            }

            throw new IllegalStateException("Incorrect raw data given to make an ImageProcessor");
        }

        @Override
        public Object make2DArray(ImageProcessor ip, boolean useBZero) {
            var lip = ((FloatProcessor) ip);

            var width = ip.getWidth();
            var height = ip.getHeight();
            // y and x are inverted because of the implementations of ImageProcessor#getPixelValue
            var outArray = new float[height][width];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    outArray[height - 1 - y][x] = lip.getf(x, y);
                }
            }

            return outArray;
        }

        @Override
        public boolean isFloatingPoint() {
            return true;
        }

        @Override
        public int getExpectedBitpix() {
            return -64;
        }
    };

    private static final boolean USE_FMA = true;

    private final BiFunction<Integer, Integer, ? extends ImageProcessor> factory;
    private final Class<?> rawDataType;

    ImageType(BiFunction<Integer, Integer, ? extends ImageProcessor> factory, Class<?> rawDataType) {
        this.factory = factory;
        this.rawDataType = rawDataType;
    }

    public static ImageType getType(Object rawData) {
        for (ImageType type : values()){
            if (type.rawDataType.isInstance(rawData)) return type;
        }
        throw new IllegalStateException("Tried to open image data that was not a numeric: " + rawData.getClass());
    }

    public static ImageType getType(ImageProcessor ip) {
        if (ip instanceof IntProcessor) {
            return INT;
        } else if (ip instanceof ShortProcessor) {
            return SHORT;
        } else if (ip instanceof ByteProcessor) {
            return BYTE;
        } else if (ip instanceof FloatProcessor) {
            return FLOAT;
        } else if (ip.getBitDepth() == 24) {
            //todo how to handle? use complex?
        }
        throw new IllegalStateException("Tried to get an image type that was not known: " + ip.getClass());
    }

    public boolean isFloatingPoint() {
        return false;
    }

    public ImageProcessor makeProcessor(int width, int height) {
        return factory.apply(width, height);
    }

    public abstract Object processImageData(Object rawData, int width, int height, double bzero, double bscale);

    public abstract Object make2DArray(ImageProcessor ip, boolean useBZero);

    public double getBZero() {
        return 0;
    }

    public abstract int getExpectedBitpix();
}
