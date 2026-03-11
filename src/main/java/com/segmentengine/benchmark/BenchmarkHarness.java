package com.segmentengine.benchmark;

import com.segmentengine.engine.CompiledSegment;
import com.segmentengine.engine.FieldAccessorRegistry;
import com.segmentengine.engine.SegmentCompiler;
import com.segmentengine.engine.SegmentEngine;
import com.segmentengine.incremental.IncrementalSegmentEngine;
import com.segmentengine.incremental.SegmentDependencyIndex;
import com.segmentengine.metrics.BenchmarkResult;
import com.segmentengine.model.Profile;
import com.segmentengine.model.ProfileUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class BenchmarkHarness {
    private final SegmentCompiler segmentCompiler;
    private final SegmentEngine segmentEngine;

    public BenchmarkHarness(SegmentCompiler segmentCompiler, SegmentEngine segmentEngine) {
        this.segmentCompiler = segmentCompiler;
        this.segmentEngine = segmentEngine;
    }

    public BenchmarkResult run(int profileCount, int segmentCount, long seed, boolean optimize) {
        SyntheticDataGenerator generator = new SyntheticDataGenerator();
        SyntheticDataSet dataSet = generator.generate(profileCount, segmentCount, seed);

        List<CompiledSegment> compiledSegments = segmentCompiler.compile(dataSet.segments(), optimize);

        long start = System.nanoTime();
        Map<String, Set<Long>> membership = segmentEngine.evaluateAllSegments(compiledSegments, dataSet.profiles());
        long evalNanos = System.nanoTime() - start;

        double evalSeconds = evalNanos / 1_000_000_000.0;
        double profilesPerSecond = profileCount / evalSeconds;
        double predicateEvaluationsPerSecond = (profileCount * (double) segmentCount * 3.0) / evalSeconds;

        double incrementalLatency = measureIncrementalLatency(compiledSegments, dataSet.profiles(), membership, seed);

        return new BenchmarkResult(
                profileCount,
                segmentCount,
                evalNanos / 1_000_000,
                profilesPerSecond,
                predicateEvaluationsPerSecond,
                incrementalLatency
        );
    }

    private double measureIncrementalLatency(
            List<CompiledSegment> compiledSegments,
            List<Profile> profiles,
            Map<String, Set<Long>> membership,
            long seed
    ) {
        IncrementalSegmentEngine incremental = new IncrementalSegmentEngine(segmentEngine, new SegmentDependencyIndex());
        Random random = new Random(seed + 7);
        List<Long> profileIds = profiles.stream().map(Profile::getId).toList();
        List<String> fields = List.of("age", "total_spent", "last_login_days");

        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long profileId = profileIds.get(random.nextInt(profileIds.size()));
            String field = fields.get(random.nextInt(fields.size()));
            double value = switch (field) {
                case "age" -> 18 + random.nextInt(53);
                case "total_spent" -> random.nextDouble() * 5000.0;
                case "last_login_days" -> random.nextInt(366);
                default -> throw new IllegalStateException("Unexpected value: " + field);
            };
            ProfileUpdate update = new ProfileUpdate(profileId, field, value);

            long start = System.nanoTime();
            incremental.applyUpdate(compiledSegments, profiles, membership, update);
            samples.add(System.nanoTime() - start);
        }

        double averageNanos = samples.stream().mapToLong(Long::longValue).average().orElse(0);
        return averageNanos / 1_000.0;
    }

    public static BenchmarkHarness defaultHarness(com.segmentengine.optimizer.AstOptimizer optimizer) {
        FieldAccessorRegistry registry = new FieldAccessorRegistry();
        SegmentEngine engine = new SegmentEngine(registry);
        SegmentCompiler compiler = new SegmentCompiler(new com.segmentengine.dsl.Parser(), optimizer);
        return new BenchmarkHarness(compiler, engine);
    }
}
