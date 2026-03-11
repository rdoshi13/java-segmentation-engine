package com.segmentengine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segmentengine.benchmark.BenchmarkHarness;
import com.segmentengine.benchmark.BenchmarkPreset;
import com.segmentengine.dsl.AstPrettyPrinter;
import com.segmentengine.dsl.ParseException;
import com.segmentengine.engine.CompiledSegment;
import com.segmentengine.engine.FieldAccessorRegistry;
import com.segmentengine.engine.SegmentCompiler;
import com.segmentengine.engine.SegmentEngine;
import com.segmentengine.incremental.IncrementalSegmentEngine;
import com.segmentengine.incremental.IncrementalUpdateResult;
import com.segmentengine.incremental.SegmentDependencyIndex;
import com.segmentengine.metrics.BenchmarkResult;
import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;
import com.segmentengine.model.SegmentDefinition;
import com.segmentengine.optimizer.AstOptimizer;
import com.segmentengine.optimizer.OptimizerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ApiServer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final AstPrettyPrinter prettyPrinter;
    private final SegmentCompiler compiler;
    private final SegmentEngine segmentEngine;
    private final AstOptimizer optimizer;

    public ApiServer(int port) throws IOException {
        this.optimizer = OptimizerFactory.defaultOptimizer();
        this.prettyPrinter = new AstPrettyPrinter();
        this.compiler = new SegmentCompiler(new com.segmentengine.dsl.Parser(), optimizer);
        this.segmentEngine = new SegmentEngine(new FieldAccessorRegistry());
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        configureRoutes();
    }

    public void start() {
        server.start();
    }

    public void stop(int delaySeconds) {
        server.stop(delaySeconds);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public static void main(String[] args) throws Exception {
        int port = parsePort(args);
        ApiServer apiServer = new ApiServer(port);
        apiServer.start();
        System.out.println("Segmentation API listening on http://localhost:" + apiServer.port());
    }

    private void configureRoutes() {
        server.createContext("/health", exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    writeError(exchange, 405, "method_not_allowed", "Only GET is allowed for /health.");
                    return;
                }
                writeJson(exchange, 200, Map.of("status", "ok"));
            } finally {
                exchange.close();
            }
        });

        server.createContext("/parse", exchange -> handleJsonPost(exchange, this::handleParse));
        server.createContext("/evaluate", exchange -> handleJsonPost(exchange, this::handleEvaluate));
        server.createContext("/incremental", exchange -> handleJsonPost(exchange, this::handleIncremental));
        server.createContext("/benchmark", exchange -> handleJsonPost(exchange, this::handleBenchmark));
    }

    private Object handleParse(byte[] requestBody) throws Exception {
        ParseRequest request = readRequest(requestBody, ParseRequest.class);
        boolean optimize = request.optimize != null && request.optimize;
        ApiInputValidator.validateSegments(request.segments);

        List<CompiledSegment> compiledSegments = compiler.compile(request.segments, optimize);
        List<Map<String, Object>> segments = new ArrayList<>();
        for (CompiledSegment compiledSegment : compiledSegments) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", compiledSegment.name());
            entry.put("original", compiledSegment.originalExpression().accept(prettyPrinter));
            if (optimize) {
                entry.put("optimized", compiledSegment.executableExpression().accept(prettyPrinter));
            }
            segments.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "parse");
        response.put("optimize", optimize);
        response.put("segments", segments);
        return response;
    }

    private Object handleEvaluate(byte[] requestBody) throws Exception {
        EvaluateRequest request = readRequest(requestBody, EvaluateRequest.class);
        boolean optimize = request.optimize != null && request.optimize;

        ApiInputValidator.validateSegments(request.segments);
        ApiInputValidator.validateProfiles(request.profiles);

        List<CompiledSegment> compiledSegments = compiler.compile(request.segments, optimize);
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiledSegments, request.profiles);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "evaluate");
        response.put("optimize", optimize);
        response.put("membership", toSerializableMembership(membership));
        return response;
    }

    private Object handleIncremental(byte[] requestBody) throws Exception {
        IncrementalRequest request = readRequest(requestBody, IncrementalRequest.class);
        boolean optimize = request.optimize != null && request.optimize;

        ApiInputValidator.validateSegments(request.segments);
        ApiInputValidator.validateProfiles(request.profiles);
        ApiInputValidator.validateUpdates(request.updates);

        List<Profile> profiles = new ArrayList<>(request.profiles);
        List<CompiledSegment> compiledSegments = compiler.compile(request.segments, optimize);
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiledSegments, profiles);

        IncrementalSegmentEngine incrementalSegmentEngine =
                new IncrementalSegmentEngine(segmentEngine, new SegmentDependencyIndex());

        List<Map<String, Object>> updates = new ArrayList<>();
        for (ProfileUpdate update : request.updates) {
            IncrementalUpdateResult updateResult = incrementalSegmentEngine
                    .applyUpdate(compiledSegments, profiles, membership, update);
            Map<String, Object> updatePayload = new LinkedHashMap<>();
            updatePayload.put("profileId", update.getProfileId());
            updatePayload.put("fieldName", update.getFieldName());
            updatePayload.put("before", sortedBooleanMap(updateResult.getBeforeMatchBySegment()));
            updatePayload.put("after", sortedBooleanMap(updateResult.getAfterMatchBySegment()));
            updatePayload.put("impactedSegments", updateResult.getImpactedSegments().stream().sorted().toList());
            updates.add(updatePayload);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "incremental");
        response.put("optimize", optimize);
        response.put("updates", updates);
        response.put("finalMembership", toSerializableMembership(membership));
        return response;
    }

    private Object handleBenchmark(byte[] requestBody) throws Exception {
        BenchmarkRequest request = readRequest(requestBody, BenchmarkRequest.class);
        boolean optimize = request.optimize != null && request.optimize;
        long seed = request.seed == null ? 42L : request.seed;

        BenchmarkPreset preset = BenchmarkPreset.fromValue(request.preset == null ? "50k" : request.preset);
        int profileCount = request.profileCount == null ? preset.profileCount() : positiveInt(request.profileCount, "profileCount");
        int segmentCount = request.segmentCount == null ? preset.segmentCount() : positiveInt(request.segmentCount, "segmentCount");

        BenchmarkHarness benchmarkHarness = BenchmarkHarness.defaultHarness(optimizer);
        BenchmarkResult result = benchmarkHarness.run(profileCount, segmentCount, seed, optimize);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "benchmark");
        response.put("preset", preset.label());
        response.put("optimize", optimize);
        response.put("seed", seed);
        response.put("profileCount", result.profileCount());
        response.put("segmentCount", result.segmentCount());
        response.put("totalEvaluationMillis", result.totalEvaluationMillis());
        response.put("profilesPerSecond", result.profilesPerSecond());
        response.put("predicateEvaluationsPerSecond", result.predicateEvaluationsPerSecond());
        response.put("avgIncrementalLatencyMicros", result.avgIncrementalLatencyMicros());
        return response;
    }

    private void handleJsonPost(HttpExchange exchange, Endpoint endpoint) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeError(exchange, 405, "method_not_allowed", "Only POST is allowed for this endpoint.");
                return;
            }

            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            Object response = endpoint.handle(requestBody);
            writeJson(exchange, 200, response);
        } catch (ParseException ex) {
            writeError(exchange, 400, "parse_error", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            writeError(exchange, 400, "validation_error", ex.getMessage());
        } catch (Exception ex) {
            writeError(exchange, 500, "internal_error", ex.getMessage());
        } finally {
            exchange.close();
        }
    }

    private <T> T readRequest(byte[] requestBody, Class<T> type) throws IOException {
        if (requestBody.length == 0) {
            throw new IllegalArgumentException("Request body must not be empty.");
        }
        return MAPPER.readValue(requestBody, type);
    }

    private Map<String, List<Long>> toSerializableMembership(Map<String, Set<Long>> membership) {
        Map<String, List<Long>> sorted = new TreeMap<>();
        for (Map.Entry<String, Set<Long>> entry : membership.entrySet()) {
            sorted.put(entry.getKey(), new TreeSet<>(entry.getValue()).stream().toList());
        }
        return sorted;
    }

    private Map<String, Boolean> sortedBooleanMap(Map<String, Boolean> map) {
        Map<String, Boolean> sorted = new TreeMap<>(Comparator.naturalOrder());
        sorted.putAll(map);
        return sorted;
    }

    private int positiveInt(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
        return value;
    }

    private static int parsePort(String[] args) {
        int port = 8080;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for --port");
                }
                port = Integer.parseInt(args[++i]);
            }
        }
        return port;
    }

    private static void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] body = MAPPER.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }

    private static void writeError(HttpExchange exchange, int statusCode, String code, String message) throws IOException {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", code);
        payload.put("message", message);
        writeJson(exchange, statusCode, payload);
    }

    private interface Endpoint {
        Object handle(byte[] requestBody) throws Exception;
    }

    private static final class ParseRequest {
        public List<SegmentDefinition> segments;
        public Boolean optimize;
    }

    private static final class EvaluateRequest {
        public List<SegmentDefinition> segments;
        public List<Profile> profiles;
        public Boolean optimize;
    }

    private static final class IncrementalRequest {
        public List<SegmentDefinition> segments;
        public List<Profile> profiles;
        public List<ProfileUpdate> updates;
        public Boolean optimize;
    }

    private static final class BenchmarkRequest {
        public String preset;
        public Integer profileCount;
        public Integer segmentCount;
        public Long seed;
        public Boolean optimize;
    }
}
