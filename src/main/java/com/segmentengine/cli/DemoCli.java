package com.segmentengine.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segmentengine.benchmark.BenchmarkHarness;
import com.segmentengine.benchmark.BenchmarkPreset;
import com.segmentengine.benchmark.BenchmarkReportWriter;
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class DemoCli {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        int code = run(args, System.out::println, System.err::println);
        if (code != 0) {
            System.exit(code);
        }
    }

    public static int run(String[] args, Consumer<String> out, Consumer<String> err) {
        if (args.length == 0 || !"demo".equals(args[0])) {
            err.accept("Usage: demo --mode <parse|evaluate|incremental|benchmark> --segments <path> --profiles <path> [--updates <path>] [--optimize] [--seed <n>]");
            return 1;
        }

        Map<String, String> options = parseOptions(args);
        String mode = options.get("mode");
        if (mode == null) {
            err.accept("Missing required option: --mode");
            return 1;
        }

        boolean optimize = options.containsKey("optimize");
        long seed = options.containsKey("seed") ? Long.parseLong(options.get("seed")) : 42L;

        AstOptimizer optimizer = OptimizerFactory.defaultOptimizer();
        SegmentCompiler compiler = new SegmentCompiler(new com.segmentengine.dsl.Parser(), optimizer, new FieldAccessorRegistry());
        SegmentEngine segmentEngine = new SegmentEngine(new FieldAccessorRegistry());
        AstPrettyPrinter printer = new AstPrettyPrinter();

        try {
            return switch (mode) {
                case "parse" -> runParseMode(options, optimize, compiler, printer, out, err);
                case "evaluate" -> runEvaluateMode(options, optimize, compiler, segmentEngine, out, err);
                case "incremental" -> runIncrementalMode(options, optimize, compiler, segmentEngine, out, err);
                case "benchmark" -> runBenchmarkMode(options, optimize, seed, optimizer, out);
                default -> {
                    err.accept("Unsupported mode: " + mode);
                    yield 2;
                }
            };
        } catch (ParseException ex) {
            err.accept("Parse error: " + ex.getMessage());
            return 3;
        } catch (IllegalArgumentException ex) {
            err.accept("Validation error: " + ex.getMessage());
            return 3;
        } catch (IOException ex) {
            err.accept("I/O error: " + ex.getMessage());
            return 3;
        } catch (Exception ex) {
            err.accept("Execution failed: " + ex.getMessage());
            return 3;
        }
    }

    private static int runParseMode(
            Map<String, String> options,
            boolean optimize,
            SegmentCompiler compiler,
            AstPrettyPrinter printer,
            Consumer<String> out,
            Consumer<String> err
    ) throws IOException {
        String segmentsPath = options.get("segments");
        if (segmentsPath == null) {
            err.accept("Missing required option for parse mode: --segments");
            return 1;
        }

        List<SegmentDefinition> segments = readSegments(Path.of(segmentsPath));
        CliInputValidator.validateSegments(segments);
        List<CompiledSegment> compiled = compiler.compile(segments, optimize);

        out.accept("MODE=parse");
        for (CompiledSegment segment : compiled) {
            out.accept("SEGMENT " + segment.name());
            out.accept("ORIGINAL " + segment.originalExpression().accept(printer));
            if (optimize) {
                out.accept("OPTIMIZED " + segment.executableExpression().accept(printer));
            }
        }
        return 0;
    }

    private static int runEvaluateMode(
            Map<String, String> options,
            boolean optimize,
            SegmentCompiler compiler,
            SegmentEngine segmentEngine,
            Consumer<String> out,
            Consumer<String> err
    ) throws IOException {
        String segmentsPath = options.get("segments");
        String profilesPath = options.get("profiles");
        if (segmentsPath == null || profilesPath == null) {
            err.accept("Missing required options for evaluate mode: --segments and --profiles");
            return 1;
        }

        List<SegmentDefinition> segments = readSegments(Path.of(segmentsPath));
        List<Profile> profiles = readProfiles(Path.of(profilesPath));
        CliInputValidator.validateSegments(segments);
        CliInputValidator.validateProfiles(profiles);
        List<CompiledSegment> compiled = compiler.compile(segments, optimize);
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiled, profiles);

        out.accept("MODE=evaluate");
        for (String name : sortedKeys(membership)) {
            out.accept(name + "=" + formatIds(membership.get(name)));
        }
        return 0;
    }

    private static int runIncrementalMode(
            Map<String, String> options,
            boolean optimize,
            SegmentCompiler compiler,
            SegmentEngine segmentEngine,
            Consumer<String> out,
            Consumer<String> err
    ) throws IOException {
        String segmentsPath = options.get("segments");
        String profilesPath = options.get("profiles");
        String updatesPath = options.get("updates");

        if (segmentsPath == null || profilesPath == null || updatesPath == null) {
            err.accept("Missing required options for incremental mode: --segments --profiles --updates");
            return 1;
        }

        List<SegmentDefinition> segments = readSegments(Path.of(segmentsPath));
        List<Profile> profiles = readProfiles(Path.of(profilesPath));
        List<ProfileUpdate> updates = readUpdates(Path.of(updatesPath));
        CliInputValidator.validateSegments(segments);
        CliInputValidator.validateProfiles(profiles);
        CliInputValidator.validateUpdates(updates);

        List<CompiledSegment> compiled = compiler.compile(segments, optimize);
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiled, profiles);

        IncrementalSegmentEngine incremental = new IncrementalSegmentEngine(segmentEngine, new SegmentDependencyIndex());

        out.accept("MODE=incremental");
        for (ProfileUpdate update : updates) {
            IncrementalUpdateResult result = incremental.applyUpdate(compiled, profiles, membership, update);
            List<String> impacted = new ArrayList<>(result.getImpactedSegments());
            Collections.sort(impacted);
            out.accept("UPDATE profileId=" + update.getProfileId() + " field=" + update.getFieldName() + " impacted=" + impacted);
            for (String segmentName : impacted) {
                boolean before = result.getBeforeMatchBySegment().get(segmentName);
                boolean after = result.getAfterMatchBySegment().get(segmentName);
                out.accept("DELTA " + segmentName + " " + before + "->" + after);
            }
        }

        out.accept("FINAL_MEMBERSHIP");
        for (String name : sortedKeys(membership)) {
            out.accept(name + "=" + formatIds(membership.get(name)));
        }
        return 0;
    }

    private static int runBenchmarkMode(
            Map<String, String> options,
            boolean optimize,
            long seed,
            AstOptimizer optimizer,
            Consumer<String> out
    ) throws IOException {
        String presetValue = options.getOrDefault("preset", "50k");
        BenchmarkPreset preset = BenchmarkPreset.fromValue(presetValue);
        int profileCount = options.containsKey("profile-count")
                ? parsePositiveInt(options.get("profile-count"), "--profile-count")
                : preset.profileCount();
        int segmentCount = options.containsKey("segment-count")
                ? parsePositiveInt(options.get("segment-count"), "--segment-count")
                : preset.segmentCount();

        BenchmarkHarness harness = BenchmarkHarness.defaultHarness(optimizer);
        BenchmarkResult result = harness.run(profileCount, segmentCount, seed, optimize);

        out.accept("MODE=benchmark");
        out.accept("preset=" + preset.label());
        out.accept("profiles=" + result.profileCount());
        out.accept("segments=" + result.segmentCount());
        out.accept("total_eval_ms=" + result.totalEvaluationMillis());
        out.accept("profiles_per_sec=" + String.format("%.2f", result.profilesPerSecond()));
        out.accept("predicate_evals_per_sec=" + String.format("%.2f", result.predicateEvaluationsPerSecond()));
        out.accept("avg_incremental_latency_micros=" + String.format("%.2f", result.avgIncrementalLatencyMicros()));

        if (options.containsKey("output")) {
            String outputPath = options.get("output");
            String format = resolveReportFormat(options, outputPath);
            BenchmarkReportWriter writer = new BenchmarkReportWriter();
            writer.write(Path.of(outputPath), format, result, seed, optimize, preset.label());
            out.accept("report_format=" + format);
            out.accept("report_path=" + Path.of(outputPath).toAbsolutePath());
        }
        return 0;
    }

    private static Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String key = arg.substring(2);
            if ("optimize".equals(key)) {
                options.put(key, "true");
                continue;
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for option: " + arg);
            }
            options.put(key, args[++i]);
        }
        return options;
    }

    private static int parsePositiveInt(String raw, String optionName) {
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Expected integer value for " + optionName + ": " + raw);
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Expected positive integer for " + optionName + ": " + raw);
        }
        return value;
    }

    private static String resolveReportFormat(Map<String, String> options, String outputPath) {
        if (options.containsKey("format")) {
            return normalizeFormat(options.get("format"));
        }
        String lower = outputPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".csv")) {
            return "csv";
        }
        if (lower.endsWith(".json")) {
            return "json";
        }
        throw new IllegalArgumentException(
                "Cannot infer report format from output path. Use --format <csv|json> or .csv/.json extension."
        );
    }

    private static String normalizeFormat(String format) {
        String normalized = format.toLowerCase(Locale.ROOT);
        if (!normalized.equals("csv") && !normalized.equals("json")) {
            throw new IllegalArgumentException("Unsupported report format: " + format + ". Use csv or json.");
        }
        return normalized;
    }

    private static List<SegmentDefinition> readSegments(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), new TypeReference<List<SegmentDefinition>>() {
        });
    }

    private static List<Profile> readProfiles(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), new TypeReference<List<Profile>>() {
        });
    }

    private static List<ProfileUpdate> readUpdates(Path path) throws IOException {
        return MAPPER.readValue(path.toFile(), new TypeReference<List<ProfileUpdate>>() {
        });
    }

    private static List<String> sortedKeys(Map<String, Set<Long>> membership) {
        List<String> keys = new ArrayList<>(membership.keySet());
        Collections.sort(keys);
        return keys;
    }

    private static String formatIds(Set<Long> ids) {
        return new TreeSet<>(ids).toString();
    }
}
