package info.kgeorgiy.ja.islamova.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.channels.*;
import java.util.regex.Pattern;

public class HelloUDPNonblockingClient implements NewHelloClient {
    @Override
    public void newRun(List<Request> requests, int threads) {
        Map<DatagramChannel, ChannelState> states = new HashMap<>();
        try (Selector selector = Selector.open()) {


            for (int i = 0; i < threads; i++) {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);

                ChannelState state = new ChannelState(channel, i + 1, requests);
                states.put(channel, state);

                if (state.hasMoreRequests()) {
                    state.prepareNextRequest();
                    channel.register(selector, SelectionKey.OP_WRITE, state);
                }
            }

            boolean flag = false;
            while (!states.isEmpty() && !Thread.interrupted()) {
                selector.select(40);

                flag = true;
                System.out.println(selector.selectedKeys().size());
                for (final Iterator<SelectionKey> i =
                     selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                        flag = false;

                        if (!key.isValid()) {
                            System.out.println("key isnt valid");
                            continue;
                        }
                        ChannelState state = (ChannelState) key.attachment();
                        System.out.println("aaaaaaaaaa");
                        if (key.interestOps() == SelectionKey.OP_READ)  {
                            System.out.println("read");
                            handleRead(key, state, states, i);
                        } else {
                            System.out.println("write");
                            handleWrite(key, state, states);
                        }
                }
            }
        } catch (IOException e) {
            System.out.println("selector cannot be open: " + e.getMessage());
        } finally {
            for (DatagramChannel channel: states.keySet()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void handleWrite(SelectionKey key, ChannelState state, Map<DatagramChannel, ChannelState> states) throws IOException {
        SocketAddress address = new InetSocketAddress(state.hostname, state.port);
        state.channel.send(state.buffer.flip(), address);
        System.out.println("Sended: " + new String(state.buffer.array()));

        key.interestOps(SelectionKey.OP_READ);
        System.out.println("key is read");
    }

    private void handleRead(SelectionKey key, ChannelState state,
                            Map<DatagramChannel, ChannelState> states, Iterator<SelectionKey> i) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        state.channel.receive(buffer);
        System.out.println(state.channel.receive(buffer));
        String response = StandardCharsets.UTF_8.decode(buffer.flip()).toString();
        System.out.println(state.expectedResponse);
        System.out.println(response);
        if (state.checkResponse(response)) {
            System.out.println(state.expectedResponse);
            System.out.println(response);

            if (state.hasMoreRequests()) {
                state.prepareNextRequest();
                key.interestOps(SelectionKey.OP_WRITE);
            } else {
                state.channel.close();
                states.remove(state.channel);
                i.remove();
            }
        }
    }

    private static class ChannelState {
        final DatagramChannel channel;
        final int threadNo;
        final List<Request> requests;
        int currentRequestIndex = 0;
        ByteBuffer buffer;
        String expectedResponse;
        String hostname;
        int port;

        ChannelState(DatagramChannel channel, int threadNo, List<Request> requests) {
            this.channel = channel;
            this.threadNo = threadNo;
            this.requests = requests;
        }

        boolean hasMoreRequests() {
            return currentRequestIndex < requests.size();
        }

        void prepareNextRequest() {
            Request request = requests.get(currentRequestIndex);
            String requestText = request.template().replaceAll("\\$", String.valueOf(threadNo));
            expectedResponse = requestText;
            System.out.println("in class "+ expectedResponse);
            buffer = ByteBuffer.wrap(requestText.getBytes(StandardCharsets.UTF_8));
            System.out.println("in class "+ buffer.flip());
            buffer.flip();
            hostname = request.host();
            port = request.port();
            currentRequestIndex++;
        }

        boolean checkResponse(String response) {
            StringBuilder res = new StringBuilder();
            for (char c: response.toCharArray()) {
                if (Character.isDigit(c)) {
                    res.append(Character.getNumericValue(c));
                } else {
                    res.append(c);
                }
            }
            String res1 = res.toString();
            Pattern pat1 = Pattern.compile(".*" + Pattern.quote(expectedResponse) + "\\d+.*");
            return !res1.matches(pat1.pattern()) && res1.contains(expectedResponse);
        }
    }

}