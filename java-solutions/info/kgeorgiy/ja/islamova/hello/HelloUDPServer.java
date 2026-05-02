package info.kgeorgiy.ja.islamova.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Implementation of {{@link HelloServer} interface
 * UDP server implementation that listens for client requests and responds to them.
 *
 */
public class HelloUDPServer implements HelloServer {

    private ExecutorService workers;
    private ExecutorService receiver;
    private DatagramSocket socket;
    private int bufferSize;
    private volatile boolean isRunning;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int port, int threads) {
        workers = Executors.newFixedThreadPool(threads);
        receiver = Executors.newSingleThreadExecutor();
        isRunning = true;
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(40);
            bufferSize = socket.getReceiveBufferSize();
            receiver.submit(this::receive);
        } catch (java.net.SocketException e) {
            System.err.println("socket cannot be open: " + e);
        }
    }

    private void receive() {

        while (isRunning && !Thread.currentThread().isInterrupted()) {
            // :NOTE: reuse packet
            DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);
            try {
                socket.receive(packet);
                try {
                    workers.submit(() -> processRequest(packet));
                } catch (RejectedExecutionException e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        System.err.println("thread cannot wait: " + e);
                    }
                }
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("error while receiving packet: " + e.getMessage());
                }
            }
        }
    }

    private void processRequest(DatagramPacket packet) {
        try {
            String request = new String(
                    packet.getData(),
                    packet.getOffset(),
                    packet.getLength(),
                    StandardCharsets.UTF_8);

            String response = "Hello, " + request;
            byte[] responseData = response.getBytes(StandardCharsets.UTF_8);

            DatagramPacket responsePacket = new DatagramPacket(
                    responseData,
                    responseData.length,
                    packet.getSocketAddress());

            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("Error processing request: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        isRunning = false;
        receiver.close();
        workers.close();
        socket.close();
    }

    /**
     * Main method to start the server from command line.
     * Expected arguments:
     * - port number
     * - number of worker threads
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Usage: HelloUDPServer <port> <threads>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            int threads = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threads);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format in arguments: " + e.getMessage());
        }
    }
}

