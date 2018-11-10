package ru.ifmo.rain.ionov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final SocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Can't find host: " + host);
            return;
        }
        final ExecutorService workersThreadPool = Executors.newFixedThreadPool(threads);
        for (int ind = 0; ind < threads; ind++) {
            final int id = ind;
            workersThreadPool.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(50);
                    final byte[] sendBuffer = new byte[0];
                    final DatagramPacket datagramPacketSend = new DatagramPacket(sendBuffer, 0, socketAddress);
                    final byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
                    final DatagramPacket datagramPacketReceive = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    for (int i = 0; i < requests; i++) {
                        boolean received = false;
                        while (!received) {
                            try {
                                final String sendText = prefix + id + "_" + i;
                                datagramPacketSend.setData(sendText.getBytes(StandardCharsets.UTF_8));
                                socket.send(datagramPacketSend);
                                System.out.println("Sending done :" + sendText);
                                socket.receive(datagramPacketReceive);
                                final String receiveText = new String(datagramPacketReceive.getData(), datagramPacketReceive.getOffset(), datagramPacketReceive.getLength(), StandardCharsets.UTF_8);
                                if ((receiveText.length() != sendText.length()) && receiveText.contains(sendText)) {
                                    received = true;
                                    System.out.println("Receive done: " + receiveText);
                                }
                            } catch (IOException e) {
                                System.err.println("An error occurred while working: " + e.getMessage());
                            }
                        }
                    }
                } catch (SocketException e) {
                    System.err.println("Can't create socket to server: " + socketAddress.toString());
                }
            });
        }
        workersThreadPool.shutdown();
        try {
            workersThreadPool.awaitTermination(threads * requests, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
    }
}