package ij.astro.util;

import ij.IJ;

import java.io.IOException;
import java.io.InputStream;

public class ProgressTrackingInputStream extends InputStream {
    private final InputStream in;
    private long inputByteCount = 0;
    private long totalSizeInBytes = 0;

    public ProgressTrackingInputStream(InputStream inputStream) {
        in = inputStream;
        try {
            totalSizeInBytes = in.available();
        } catch (IOException ignored) {
        }
    }

    public long getInputByteCount() {
        return inputByteCount;
    }

    public long getTotalSizeInBytes() {
        return totalSizeInBytes;
    }

    public void setTotalSizeInBytes(long totalSizeInBytes) {
        this.totalSizeInBytes = totalSizeInBytes;
    }

    @Override
    public int read() throws IOException {
        incrementByteCount();
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        incrementByteCount(len);
        return in.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        var b = in.readAllBytes();
        incrementByteCount(b.length);
        return b;
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        var b = in.readNBytes(len);
        incrementByteCount(b.length);
        return b;
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        var i = in.readNBytes(b, off, len);
        incrementByteCount(i);
        return i;
    }

    @Override
    public long skip(long n) throws IOException {
        incrementByteCount(n);
        return in.skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        incrementByteCount(n);
        in.skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    private void incrementByteCount() {
        incrementByteCount(1);
    }

    private void incrementByteCount(long len) {
        inputByteCount += len;

        if (totalSizeInBytes > 0) {
            IJ.showProgress(inputByteCount / (double) totalSizeInBytes);
        }
    }
}
