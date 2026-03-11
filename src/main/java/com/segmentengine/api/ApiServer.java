package com.segmentengine.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segmentengine.api.dto.ApiErrorResponse;
import com.segmentengine.api.dto.BenchmarkRequest;
import com.segmentengine.api.dto.BenchmarkResponse;
import com.segmentengine.api.dto.EvaluateRequest;
import com.segmentengine.api.dto.EvaluateResponse;
import com.segmentengine.api.dto.HealthResponse;
import com.segmentengine.api.dto.IncrementalRequest;
import com.segmentengine.api.dto.IncrementalResponse;
import com.segmentengine.api.dto.IncrementalUpdateResponse;
import com.segmentengine.api.dto.ParseRequest;
import com.segmentengine.api.dto.ParseResponse;
import com.segmentengine.api.dto.ParseSegmentResponse;
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
import com.segmentengine.optimizer.AstOptimizer;
import com.segmentengine.optimizer.OptimizerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        this.compiler = new SegmentCompiler(new com.segmentengine.dsl.Parser(), optimizer, new FieldAccessorRegistry());
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
        registerHealthRoute("/v1/health");
        registerHealthRoute("/health");

        registerJsonRoute("/v1/parse", this::handleParse);
        registerJsonRoute("/v1/evaluate", this::handleEvaluate);
        registerJsonRoute("/v1/incremental", this::handleIncremental);
        registerJsonRoute("/v1/benchmark", this::handleBenchmark);

        // Compatibility aliases.
        registerJsonRoute("/parse", this::handleParse);
        registerJsonRoute("/evaluate", this::handleEvaluate);
        registerJsonRoute("/incremental", this::handleIncremental);
        registerJsonRoute("/benchmark", this::handleBenchmark);

        server.createContext("/", this::handleNotFound);
    }

    private void registerHealthRoute(String path) {
        server.createContext(path, exchange -> {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    writeError(
                            exchange,
                            405,
                            "method_not_allowed",
                            "Only GET is allowed for this endpoint.",
                            Map.of("path", path, "allowed", List.of("GET"))
                    );
                    return;
                }
                writeJson(exchange, 200, new HealthResponse("ok"));
            } finally {
                exchange.close();
            }
        });
    }

    private void registerJsonRoute(String path, Endpoint endpoint) {
        server.createContext(path, exchange -> handleJsonPost(exchange, endpoint, path));
    }

    private ParseResponse handleParse(byte[] requestBody) throws Exception {
        ParseRequest request = readRequest(requestBody, ParseRequest.class);
        boolean optimize = Boolean.TRUE.equals(request.optimize());
        ApiInputValidator.validateSegments(request.segments());

        List<CompiledSegment> compiledSegments = compiler.compile(request.segments(), optimize);
        List<ParseSegmentResponse> segments = new ArrayList<>();
        for (CompiledSegment compiledSegment : compiledSegments) {
            segments.add(new ParseSegmentResponse(
                    compiledSegment.name(),
                    compiledSegment.originalExpression().accept(prettyPrinter),
                    optimize ? compiledSegment.executableExpression().accept(prettyPrinter) : null
            ));
        }

        return new ParseResponse("parse", optimize, segments);
    }

    private EvaluateResponse handleEvaluate(byte[] requestBody) throws Exception {
        EvaluateRequest request = readRequest(requestBody, EvaluateRequest.class);
        boolean optimize = Boolean.TRUE.equals(request.optimize());

        ApiInputValidator.validateSegments(request.segments());
        ApiInputValidator.validateProfiles(request.profiles());

        List<CompiledSegment> compiledSegments = compiler.compile(request.segments(), optimize);
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiledSegments, request.profiles());

        return new EvaluateResponse("evaluate", optimize, toSerializableMembership(membership));
    }

    private IncrementalResponse handleIncremental(byte[] requestBody) throws Exception {
        IncrementalRequest request = readRequest(requestBody, IncrementalRequest.class);
        boolean optimize = Boolean.TRUE.equals(request.optimize());

        ApiInputValidator.validateSegments(request.segments());
        ApiInputValidator.validateProfiles(request.profiles());
        ApiInputValidator.validateUpdates(request.updates());

        List<Profile> profiles = new ArrayList<>(request.profiles());
        List<CompiledSegment> compiledSegments = compiler.compile(request.segments(), optimize);
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiledSegments, profiles);

        IncrementalSegmentEngine incrementalSegmentEngine =
                new IncrementalSegmentEngine(segmentEngine, new SegmentDependencyIndex());

        List<IncrementalUpdateResponse> updates = new ArrayList<>();
        for (ProfileUpdate update : request.updates()) {
            IncrementalUpdateResult updateResult = incrementalSegmentEngine
                    .applyUpdate(compiledSegments, profiles, membership, update);
            updates.add(new IncrementalUpdateResponse(
                    update.getProfileId(),
                    update.getFieldName(),
                    sortedBooleanMap(updateResult.getBeforeMatchBySegment()),
                    sortedBooleanMap(updateResult.getAfterMatchBySegment()),
                    updateResult.getImpactedSegments().stream().sorted().toList()
            ));
        }

        return new IncrementalResponse("incremental", optimize, updates, toSerializableMembership(membership));
    }

    private BenchmarkResponse handleBenchmark(byte[] requestBody) throws Exception {
        BenchmarkRequest request = readRequest(requestBody, BenchmarkRequest.class);
        boolean optimize = Boolean.TRUE.equals(request.optimize());
        long seed = request.seed() == null ? 42L : request.seed();

        BenchmarkPreset preset = BenchmarkPreset.fromValue(request.preset() == null ? "50k" : request.preset());
        int profileCount = request.profileCount() == null ? preset.profileCount() : positiveInt(request.profileCount(), "profileCount");
        int segmentCount = request.segmentCount() == null ? preset.segmentCount() : positiveInt(request.segmentCount(), "segmentCount");

        BenchmarkHarness benchmarkHarness = BenchmarkHarness.defaultHarness(optimizer);
        BenchmarkResult result = benchmarkHarness.run(profileCount, segmentCount, seed, optimize);

        return new BenchmarkResponse(
                "benchmark",
                preset.label(),
                optimize,
                seed,
                result.profileCount(),
                result.segmentCount(),
                result.totalEvaluationMillis(),
                result.profilesPerSecond(),
                result.predicateEvaluationsPerSecond(),
                result.avgIncrementalLatencyMicros()
        );
    }

    private void handleJsonPost(HttpExchange exchange, Endpoint endpoint, String path) throws IOException {
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                writeError(
                        exchange,
                        405,
                        "method_not_allowed",
                        "Only POST is allowed for this endpoint.",
                        Map.of("path", path, "allowed", List.of("POST"))
                );
                return;
            }

            if (!isJsonRequest(exchange)) {
                writeError(
                        exchange,
                        415,
                        "unsupported_media_type",
                        "Content-Type must be application/json.",
                        Map.of("path", path)
                );
                return;
            }

            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            Object response = endpoint.handle(requestBody);
            writeJson(exchange, 200, response);
        } catch (JsonProcessingException ex) {
            writeError(exchange, 400, "invalid_json", "Malformed JSON request body.", Map.of("detail", ex.getOriginalMessage()));
        } catch (ParseException ex) {
            writeError(exchange, 400, "parse_error", ex.getMessage(), Map.of("path", path));
        } catch (IllegalArgumentException ex) {
            writeError(exchange, 400, "validation_error", ex.getMessage(), Map.of("path", path));
        } catch (Exception ex) {
            writeError(
                    exchange,
                    500,
                    "internal_error",
                    "Unhandled server error.",
                    Map.of("path", path, "exception", ex.getClass().getSimpleName())
            );
        } finally {
            exchange.close();
        }
    }

    private void handleNotFound(HttpExchange exchange) throws IOException {
        try {
            writeError(
                    exchange,
                    404,
                    "route_not_found",
                    "Route not found.",
                    Map.of("path", exchange.getRequestURI().getPath())
            );
        } finally {
            exchange.close();
        }
    }

    private boolean isJsonRequest(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null) {
            return false;
        }
        return contentType.toLowerCase(Locale.ROOT).startsWith("application/json");
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

    private static void writeError(
            HttpExchange exchange,
            int statusCode,
            String code,
            String message,
            Map<String, Object> details
    ) throws IOException {
        ApiErrorResponse payload = new ApiErrorResponse(code, message, details == null ? Map.of() : new LinkedHashMap<>(details));
        writeJson(exchange, statusCode, payload);
    }

    private interface Endpoint {
        Object handle(byte[] requestBody) throws Exception;
    }
}
