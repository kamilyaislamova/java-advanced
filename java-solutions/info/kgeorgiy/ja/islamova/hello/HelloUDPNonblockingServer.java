package info.kgeorgiy.ja.islamova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements HelloServer {
    private ExecutorService workers;
    private ExecutorService receiver;
    private DatagramChannel channel;
    private Selector selector;
    private volatile boolean isRunning;
    ConcurrentLinkedQueue<Pair<SocketAddress, ByteBuffer>> storage = new ConcurrentLinkedQueue<>();

    @Override
    public void start(int port, int threads) {
        workers = Executors.newFixedThreadPool(threads);
        receiver = Executors.newSingleThreadExecutor();
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));

            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);

            isRunning = true;

            receiver.submit(this::run);
        } catch (IOException e) {
            System.err.println("error while starting server: " + e.getMessage());
        }
    }

    private void run() {
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                selector.select(250);
                for (final Iterator<SelectionKey> i =
                     selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                    try {
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            SocketAddress address = channel.receive(buffer);
                            workers.submit(() -> processRequest(address, buffer));
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                        if (key.isWritable()) {
                            if (!storage.isEmpty()) {
                                Pair<SocketAddress, ByteBuffer> pair = storage.stream().findAny().get();
                                channel.send(pair.second, pair.first);
                                storage.remove(pair);
                            }
                            key.interestOpsOr(SelectionKey.OP_READ);
                        }
                    } finally {
                        i.remove();
                    }
                }
            } catch (IOException e) {
                System.err.println("cannot select channel keys from selector: " + e.getMessage());
            }

        }
    }

    private void processRequest(SocketAddress address, ByteBuffer buffer) {
        if (address != null) {
            buffer.flip();
            String request = StandardCharsets.UTF_8.decode(buffer).toString();
            String response = "Hello, " + request;

            buffer.clear();
            buffer.put(response.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            storage.add(new Pair<>(address, buffer));
        }
    }

    @Override
    public void close() {
        isRunning = false;
        receiver.close();
        workers.close();
        try {
            selector.close();
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record Pair<T, R>(T first, R second) {
    }
}
