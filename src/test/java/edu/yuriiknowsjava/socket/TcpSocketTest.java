package edu.yuriiknowsjava.socket;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class TcpSocketTest {

    @Test
    void openServerSocket() throws Exception {
        int port = 9218;
        ServerSocket serverSocket = createServerSocket(port);
        for (int i = 0; i < 3; i++) {
            if (serverSocket != null && serverSocket.isBound()) {
                break;
            } else {
                TimeUnit.SECONDS.sleep(2);
            }
        }

        var inetAddress = Inet4Address.getByName("localhost");
        try (var socket = new Socket(inetAddress, port);
             var outputStream = new DataOutputStream(socket.getOutputStream());
             var inputStream = new DataInputStream(socket.getInputStream())) {
            outputStream.writeUTF("Hello from client");
            System.out.println("Server response: " + inputStream.readUTF());
            System.out.println();
            outputStream.writeUTF("Second msg from client");
            System.out.println("Second Server response: " + inputStream.readUTF());
        }
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    private static ServerSocket createServerSocket(int port) {
        AtomicReference<ServerSocket> serverSocketRef = new AtomicReference<>();
        var serverThread = new Thread(() -> {
            try {
                serverSocketRef.set(new ServerSocket(port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (var socket = serverSocketRef.get().accept(); // blocks until a clients connects
                 var outputStream = new DataOutputStream(socket.getOutputStream());
                 var inputStream = new DataInputStream(socket.getInputStream())) {
                System.out.println("Client request: " + inputStream.readUTF());
                outputStream.writeUTF("Hello from server");
                System.out.println();
                System.out.println("Second client request: " + inputStream.readUTF());
                outputStream.writeUTF("Second msg from server");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        return serverSocketRef.get();
    }
}
