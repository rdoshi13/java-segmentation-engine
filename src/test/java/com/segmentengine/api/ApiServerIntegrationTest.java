package com.segmentengine.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerIntegrationTest {
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
    void parseEndpointReturnsExpressions() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("segments", loadJsonList("demo/segments.json"));
        payload.put("optimize", true);

        HttpResponse<String> response = postJson("/parse", payload);
        assertEquals(200, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("parse", json.get("mode").asText());
        assertTrue(json.get("segments").isArray());
        assertTrue(json.get("segments").get(0).has("optimized"));
    }

    @Test
    void evaluateEndpointReturnsMembership() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("segments", loadJsonList("demo/segments.json"));
        payload.put("profiles", loadJsonList("demo/profiles.json"));

        HttpResponse<String> response = postJson("/evaluate", payload);
        assertEquals(200, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("evaluate", json.get("mode").asText());
        assertEquals("[1,4]", json.get("membership").get("high_value").toString().replace(" ", ""));
    }

    @Test
    void incrementalEndpointReturnsFinalMembership() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("segments", loadJsonList("demo/segments.json"));
        payload.put("profiles", loadJsonList("demo/profiles.json"));
        payload.put("updates", loadJsonList("demo/updates.json"));

        HttpResponse<String> response = postJson("/incremental", payload);
        assertEquals(200, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("incremental", json.get("mode").asText());
        assertEquals("[1]", json.get("finalMembership").get("high_value").toString().replace(" ", ""));
    }

    @Test
    void benchmarkEndpointSupportsPresetAndOverrides() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("preset", "100k");
        payload.put("profileCount", 200);
        payload.put("segmentCount", 20);
        payload.put("seed", 7);

        HttpResponse<String> response = postJson("/benchmark", payload);
        assertEquals(200, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("benchmark", json.get("mode").asText());
        assertEquals("100k", json.get("preset").asText());
        assertEquals(200, json.get("profileCount").asInt());
        assertEquals(20, json.get("segmentCount").asInt());
    }

    @Test
    void returnsValidationErrorForBadPayload() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("segments", List.of(Map.of("name", "dup", "rule", "age > 20"), Map.of("name", "dup", "rule", "age > 30")));
        payload.put("profiles", loadJsonList("demo/profiles.json"));

        HttpResponse<String> response = postJson("/evaluate", payload);
        assertEquals(400, response.statusCode());

        JsonNode json = MAPPER.readTree(response.body());
        assertEquals("validation_error", json.get("error").asText());
    }

    private HttpResponse<String> postJson(String path, Object payload) throws Exception {
        String body = MAPPER.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private List<Map<String, Object>> loadJsonList(String resourceName) throws IOException {
        Path path;
        try {
            path = Path.of(getClass().getClassLoader().getResource(resourceName).toURI());
        } catch (Exception ex) {
            throw new IOException("Failed to load resource: " + resourceName, ex);
        }
        return MAPPER.readValue(path.toFile(), new TypeReference<>() {
        });
    }
}
