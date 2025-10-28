package ij.astro.ipc;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class IPCServer implements AutoCloseable, Closeable {
    private final ServerSocketChannel serverChannel;
    private final Path socketPath;
    private final Consumer<String> handler;
    private final ExecutorService acceptor;
    private final ExecutorService workers;
    private volatile boolean running = true;

    public IPCServer(Path socketPath, Consumer<String> handler) throws IOException {
        this.socketPath = socketPath;
        this.handler = handler;

        serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        var addr = UnixDomainSocketAddress.of(socketPath);
        serverChannel.bind(addr);

        acceptor = Executors.newSingleThreadExecutor(r -> {
            var t = new Thread(r, "IPCServer-Acceptor");
            t.setDaemon(true);
            return t;
        });
        workers = Executors.newCachedThreadPool();

        startAccepting();
    }

    private void startAccepting() {
        acceptor.submit(() -> {
            try {
                while (running && serverChannel.isOpen()) {
                    try {
                        var client = serverChannel.accept();
                        workers.submit(() -> handleClient(client));
                    } catch (IOException e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            } finally {
                try { close(); } catch (IOException ignored) {}
            }
        });
    }

    private void handleClient(SocketChannel clientChannel) {
        try (SocketChannel cc = clientChannel;
             BufferedReader in = new BufferedReader(new InputStreamReader(Channels.newInputStream(cc), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handler.accept(line);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        running = false;
        try {
            serverChannel.close();
        } catch (IOException ignored) {}
        acceptor.shutdownNow();
        workers.shutdownNow();

        try { Files.deleteIfExists(socketPath); } catch (IOException ignored) {}
    }
}
