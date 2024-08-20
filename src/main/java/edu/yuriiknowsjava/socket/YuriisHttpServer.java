package edu.yuriiknowsjava.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class YuriisHttpServer implements AutoCloseable {
    private final int port;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);
    private final AtomicInteger counter = new AtomicInteger(0);
    private final ExecutorService executorService;

    private ServerSocket server;

    public YuriisHttpServer(int port, int poolSize) {
        this.port = port;
        executorService = Executors.newFixedThreadPool(poolSize, task -> {
            Thread thread = new Thread(task);
            thread.setName("YuriisHttpServer-" + counter.incrementAndGet());
            return thread;
        });
    }

    public void run() {
        try {
            server = new ServerSocket(port);
            isOpen.set(true);
            while (isOpen.get()) {
                Socket socket = server.accept();
                executorService.execute(() -> processSocket(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isReady() {
        return server != null
                && !server.isClosed()
                && server.isBound();
    }

    private void processSocket(Socket socket) {
        try (socket;
             var inputStream = new DataInputStream(socket.getInputStream());
             var outputStream = new DataOutputStream(socket.getOutputStream())) {
            var req = new String(inputStream.readNBytes(128)); // blocks until the number of bytes is read!
            TimeUnit.SECONDS.sleep(2); // TODO: some work

            // language=HTML
            var respBody = """
                    <html lang="en">
                       <head>
                           <title>Yurii's HTTP server</title>
                       </head>
                       <body>
                           <div>Read first 128 bytes from request:
                               <p>"%s"</p>
                           </div>
                           <p>Response from Yurii's server</p>
                       </body>
                    </html>
                    """.formatted(req.replaceAll("\n|\r", " ")).getBytes();

            var respHeaders = """
                    HTTP/1.1 200 OK
                    content-type: text/html;charset=utf-8
                    content-length: %s
                    """.formatted(respBody.length).getBytes();
            outputStream.write(respHeaders);
            outputStream.write(System.lineSeparator().getBytes());
            outputStream.write(respBody);
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
        } finally {
            System.out.println("Finish processing request from thread " + Thread.currentThread().getName());
        }
    }

    @Override
    public void close() throws Exception {
        isOpen.set(false);
        executorService.shutdown();
        boolean isTerminated = executorService.awaitTermination(5, TimeUnit.SECONDS);
        if (!isTerminated) {
            executorService.shutdownNow();
        }
        if (server != null) {
            server.close();
        }
        executorService.close();
    }
}
