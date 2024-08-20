package edu.yuriiknowsjava.socket;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

class UdpSocketTest {

    @Test
    void test() throws Exception {
        // TCP and UPD ports can coexist on the same machine
        int port = 9218;

        InetAddress localhost = Inet4Address.getByName("localhost");
        var client = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try (var datagramSocket = new DatagramSocket()) {
                    var bytes = ("Hello from UDP client, req#" + i).getBytes();
                    // UDP does not check if the server exists
                    var packet = new DatagramPacket(bytes, bytes.length, localhost, port);
                    datagramSocket.send(packet);
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        client.start();

        var server = new Thread(() -> {
            try (var datagramSocket = new DatagramSocket(port)) { // waits until a client connects
                for (int i = 0; i < 10; i++) {
                    var bytes = new byte[1024];
                    var datagramPacket = new DatagramPacket(bytes, bytes.length);
                    datagramSocket.receive(datagramPacket);
                    int length = bytes.length;
                    for (int j = 0; j < bytes.length; j++) {
                        if (bytes[j] == 0) {
                            length = j;
                            break;
                        }
                    }
                    System.out.println(new String(bytes, 0, length));
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        server.start();
        TimeUnit.SECONDS.sleep(2);
    }
}
