package edu.yuriiknowsjava.socket;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

class SunHttp {
    private static String text;

    static {
        // When you want to use virtual threads, but don't know if the 3d party libraries
        // are causing the pin behaviour (they use synchronize blocks or JNI).
        // This option will log all pinned virtual threads.
        System.setProperty("jdk.tracePinnedThreads", "full");
    }

    private final HttpServer httpServer;

    SunHttp(HttpServer httpServer) {
        this.httpServer = httpServer;
    }

    public static void main(String[] args) throws IOException {
        var mobyUrl = SunHttp.class.getResource("/moby.txt");
        if (mobyUrl == null) {
            System.out.println("No moby.txt");
            throw new RuntimeException("No moby.txt");
        }
        text = Files.readString(Paths.get(mobyUrl.getPath()));

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(1888), 0);
        var sunHttp = new SunHttp(httpServer);
        sunHttp.run();
    }

    public void run() throws IOException {
        httpServer.createContext("/search", this::handleSearch);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();
    }

    private void handleSearch(HttpExchange exchange) throws IOException {
        var params = exchange.getRequestURI().getQuery()
                .split("=");
        if (params.length < 2) {
            exchange.sendResponseHeaders(400, 0);
            return;
        }
        String action = params[0].toLowerCase();
        if (!action.equals("word")) {
            exchange.sendResponseHeaders(400, 0);
        }
        String word = params[1];
        long wordCount = wordCount(word);

        // language=JSON
        var response = """
                { "count" : %s }
                """.formatted(wordCount).getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        OutputStream responseBody = exchange.getResponseBody();
        try (responseBody) {
            responseBody.write(response);
        }
    }

    private long wordCount(String word) {
        long count = 0;
        int idx = 0;
        while (idx >= 0) {
            idx = text.indexOf(word, idx);
            if (idx >= 0) {
                count++;
                idx++;
            }
        }
        return count;
    }
}
