package info.kgeorgiy.ja.islamova.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Implementation of {{@link NewHelloClient} interface
 * UDP client implementation that sends requests to a server and processes responses.
 */

public class HelloUDPClient implements NewHelloClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public void newRun(List<Request> requests, int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int threadNo = 1; threadNo <= threads; threadNo++) {
            final int finalThreadNo = threadNo;
            executor.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    for (Request request : requests) {
                        processRequest(request, finalThreadNo, socket);
                    }
                } catch (SocketException e) {
                    System.err.println("error while creating socket: " + e);
                }
            });
        }

        executor.close();
    }

    private void processRequest(Request request, int threadNo, DatagramSocket socket) {
        String requestText = request.template().replaceAll("\\$", String.valueOf(threadNo));
        String responseText = sendAndReceive(request.host(), request.port(), requestText, socket);
        System.out.println(requestText);
        System.out.println(responseText);
    }

    private String sendAndReceive(String host, int port, String requestText, DatagramSocket socket) {
        //DatagramPacket requestPacket;
        // :NOTE: response packet reuse
        //DatagramPacket responsePacket;
        //InetAddress address;
        try {
            socket.setSoTimeout(40);
            InetAddress address = InetAddress.getByName(host);
            while (true) {
                try {
                    byte[] requestBytes = requestText.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket requestPacket = new DatagramPacket(
                            requestBytes, requestBytes.length, address, port);

                    byte[] responseBuffer = new byte[socket.getReceiveBufferSize()];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.send(requestPacket);
                    socket.receive(responsePacket);

                    String response = new String(
                            responsePacket.getData(),
                            responsePacket.getOffset(),
                            responsePacket.getLength(),
                            StandardCharsets.UTF_8);

                    if (check(response, requestText)) {
                        return response;
                    }
                } catch (SocketTimeoutException e) {
                    System.err.println("timeout exceeded for request: " + requestText);
                } catch (IOException e) {
                    System.err.println("error during request: " + e.getMessage());
                }
            }
            } catch(UnknownHostException e){
                System.err.println("unknown host: " + e.getMessage());
            } catch(SocketException e){
                System.err.println("socket host: " + e.getMessage());
            }
        return null;
    }

    private boolean check (String response, String request) {
        StringBuilder res = new StringBuilder();
        for (char c: response.toCharArray()) {
            if (Character.isDigit(c)) {
               res.append(Character.getNumericValue(c));
            } else {
                res.append(c);
            }
        }
        String res1 = res.toString();
        Pattern pat1 = Pattern.compile(".*" + Pattern.quote(request) + "\\d+.*");
        return !res1.matches(pat1.pattern()) && res1.contains(request);
    }

    /**
     * Main method for command-line interface.
     *
     * Expected arguments:
     * - server hostname or IP address
     * - server port number
     * - request prefix string
     * - number of parallel threads
     * - number of requests per thread
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Usage: HelloUDPClient <host> <port> <prefix> <threads> <requests>");
            return;
        }

        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String prefix = args[2];
            int threads = Integer.parseInt(args[3]);
            int requests = Integer.parseInt(args[4]);

            HelloUDPClient client = new HelloUDPClient();
            client.run(host, port, prefix, requests, threads);
        } catch (NumberFormatException e) {
            System.err.println("invalid number format in arguments: " + e.getMessage());
        }
    }


}
