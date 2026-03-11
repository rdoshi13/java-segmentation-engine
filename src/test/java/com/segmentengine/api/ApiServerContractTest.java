package com.segmentengine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerContractTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiServer apiServer;
    private HttpClient httpClient;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        apiServer = new ApiServer(0);
        apiServer.start();
        httpClient = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + apiServer.port();
    }

    @AfterEach
    void tearDown() {
        if (apiServer != null) {
            apiServer.stop(0);
        }
    }

    @Test
    void returnsInvalidJsonError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/evaluate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{bad-json"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("invalid_json", json.get("code").asText());
        assertTrue(json.has("details"));
    }

    @Test
    void returnsUnsupportedMediaTypeError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/evaluate"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(415, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("unsupported_media_type", json.get("code").asText());
    }

    @Test
    void returnsRouteNotFoundError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/unknown"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("route_not_found", json.get("code").asText());
    }

    @Test
    void returnsMethodNotAllowedError() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/evaluate"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("method_not_allowed", json.get("code").asText());
        assertTrue(json.get("details").has("allowed"));
    }
}
