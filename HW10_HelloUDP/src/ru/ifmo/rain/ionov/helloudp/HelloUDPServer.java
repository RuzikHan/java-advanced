package ru.ifmo.rain.ionov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService workersThreadPool;
    private ExecutorService oneThreadExecutor;
    private boolean closed;

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Can't create socket with port:" + port);
            return;
        }
        oneThreadExecutor = Executors.newSingleThreadExecutor();
        workersThreadPool = Executors.newFixedThreadPool(threads);
        closed = false;
        oneThreadExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
                    final DatagramPacket datagramPacketReceive = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(datagramPacketReceive);
                    workersThreadPool.submit(() -> {
                        final String sendText = new String(datagramPacketReceive.getData(), datagramPacketReceive.getOffset(), datagramPacketReceive.getLength(), StandardCharsets.UTF_8);
                        try {
                            final byte[] sendBuffer = new byte[0];
                            final DatagramPacket datagramPacketSend = new DatagramPacket(sendBuffer, 0, datagramPacketReceive.getSocketAddress());
                            datagramPacketSend.setData(("Hello, " + sendText).getBytes(StandardCharsets.UTF_8));
                            socket.send(datagramPacketSend);
                        } catch (IOException e) {
                            if (!closed) {
                                System.err.println("An error occurred while working: " + e.getMessage());
                            }
                        }
                    });
                } catch (IOException e) {
                    if (!closed) {
                        System.err.println("An error occurred while working: " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        socket.close();
        oneThreadExecutor.shutdownNow();
        workersThreadPool.shutdownNow();
    }
}