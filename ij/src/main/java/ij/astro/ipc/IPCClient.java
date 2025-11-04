package ij.astro.ipc;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class IPCClient implements AutoCloseable {
    private final Path socketPath;
    private final SocketChannel socketChannel;
    private static final int MAX_ATTEMPTS = 6;
    private static final long RETRY_DELAY = 150;

    public IPCClient(Path socketPath) throws IOException {
        this.socketPath = socketPath;
        socketChannel = setupChannel();
    }

    private SocketChannel setupChannel() throws IllegalStateException {
        var address = UnixDomainSocketAddress.of(socketPath);
        SocketChannel channel;

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try {
                channel = SocketChannel.open(address);
                return channel;
            } catch (IOException e) {
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ignored) {}
            }
        }

        throw new IllegalStateException("Failed to connect to socket");
    }

    public void sendMessage(String s) throws IOException {
        synchronized (socketChannel) {
            ByteBuffer buf = ByteBuffer.wrap((s + "\n").getBytes(StandardCharsets.UTF_8));

            while (buf.hasRemaining()) {
                socketChannel.write(buf);
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (socketChannel) {
            socketChannel.close();
        }
    }
}
