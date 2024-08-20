package edu.yuriiknowsjava.socket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

class HttpServerTest {
    private static final int PORT = 43656;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final YuriisHttpServer yuriisHttpServer = new YuriisHttpServer(PORT, 3);

    private final List<AutoCloseable> resources = new ArrayList<>();

    @BeforeEach
    void createHttpServer() {
        resources.add(httpClient);
        resources.add(yuriisHttpServer);
    }

    @AfterEach
    void closeResources() {
        List<Exception> exceptions = new ArrayList<>();
        for (AutoCloseable resource : resources.stream().filter(Objects::nonNull).toList()) {
            try {
                resource.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            fail("Exception occurred on resource closing", exceptions.getFirst());
        }
    }

    @Test
    void testHttpServer() throws Exception {
        var serverThread = new Thread(yuriisHttpServer::run);
        serverThread.setName("[Yurii's server thread from test]");
        serverThread.start();
        for (int i = 0; i < 3; i++) {
            if (yuriisHttpServer.isReady()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
        }

        // language=JSON
        var reqBody = """
                { "msg": "hello from client", "reqId": "%s" }
                """;

        var requests = Stream.iterate(1, i -> i + 1)
                .limit(5)
                .map(i -> HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody.formatted(i)))
                        .setHeader("Content-Type", "application/json")
                        .uri(URI.create("http://localhost:%s".formatted(PORT)))
                        .build())
                .map(req -> httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()))
                .toList();

        List<HttpResponse<String>> responses = requests.stream()
                .map(CompletableFuture::join)
                .toList();
        responses.forEach(resp -> {
            assertEquals(200, resp.statusCode());
            assertFalse(resp.body().isBlank());
        });

        System.out.println();
        System.out.println(responses.getFirst().headers());
        System.out.println(responses.getFirst().body());
    }
}
