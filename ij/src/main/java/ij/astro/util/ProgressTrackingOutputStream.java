package ij.astro.util;

import ij.IJ;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressTrackingOutputStream extends OutputStream {
    private final OutputStream out;
    private long outputByteCount = 0;
    private long totalSizeInBytes = 0;

    public ProgressTrackingOutputStream(OutputStream outputStream) {
        out = outputStream;
    }

    public long getOutputByteCount() {
        return outputByteCount;
    }

    public long getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    public void setTotalSizeInBytes(long totalSizeInBytes) {
        this.totalSizeInBytes = totalSizeInBytes;
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        incrementByteCount();
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        incrementByteCount(len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private void incrementByteCount() {
        incrementByteCount(1);
    }

    private void incrementByteCount(long len) {
        outputByteCount += len;

        if (totalSizeInBytes > 0) {
            IJ.showProgress(outputByteCount / (double) totalSizeInBytes);
        }
    }
}
