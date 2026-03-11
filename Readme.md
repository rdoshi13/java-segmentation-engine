# Real-Time Segmentation Query Engine

A Java 17 prototype for rule-based customer segmentation with parsing, optimization, evaluation, incremental updates, and benchmarking.

## Current Status

Implemented and runnable.

- Maven project with tests (`mvn test`)
- Stable `demo` CLI with `parse`, `evaluate`, `incremental`, and `benchmark` modes
- Deterministic test snapshots for CLI outputs

## Problem

Modern systems hold large user datasets, and product/marketing teams define segment rules such as:

```txt
age > 25 AND total_spent > 1000 AND last_login_days < 30
```

At scale, recomputing every segment for every profile after every update is expensive.

## Goal

This engine supports:

1. Parse a DSL for segment rules
2. Build an AST and execution model
3. Apply rule-based optimizations
4. Evaluate profiles against segments
5. Recompute only impacted segments after profile updates

## DSL Convention

Canonical field names in DSL will use `snake_case`:

- `age`
- `total_spent`
- `last_login_days`

Java model fields will use `camelCase`:

- `age`
- `totalSpent`
- `lastLoginDays`

Rules referencing unsupported DSL fields are rejected at compile time with validation errors.

## Architecture

```txt
Segment DSL
  -> Tokenizer
  -> Parser
  -> AST
  -> Optimizer
  -> Evaluation Engine
  -> Segment Membership Results
```

## Modules

```txt
src/main/java/com/segmentengine/
  dsl/
  engine/
  optimizer/
  incremental/
  model/
  cli/
  api/
  benchmark/
  metrics/
```

## Run

Run tests:

```bash
mvn test
```

Short wrapper script:

```bash
./scripts.sh --mode evaluate --segments src/test/resources/demo/segments.json --profiles src/test/resources/demo/profiles.json
```

On first run, the script builds classes and copies runtime dependencies automatically.

Run parse mode:

```bash
mvn -q exec:java -Dexec.mainClass=com.segmentengine.cli.DemoCli -Dexec.args="demo --mode parse --segments src/test/resources/demo/segments.json --optimize"
```

Run evaluate mode:

```bash
mvn -q exec:java -Dexec.mainClass=com.segmentengine.cli.DemoCli -Dexec.args="demo --mode evaluate --segments src/test/resources/demo/segments.json --profiles src/test/resources/demo/profiles.json"
```

Run incremental mode:

```bash
mvn -q exec:java -Dexec.mainClass=com.segmentengine.cli.DemoCli -Dexec.args="demo --mode incremental --segments src/test/resources/demo/segments.json --profiles src/test/resources/demo/profiles.json --updates src/test/resources/demo/updates.json"
```

Run benchmark mode:

```bash
mvn -q exec:java -Dexec.mainClass=com.segmentengine.cli.DemoCli -Dexec.args="demo --mode benchmark --profile-count 50000 --segment-count 100 --seed 42"
```

Run benchmark mode with preset and report export:

```bash
mvn -q exec:java -Dexec.mainClass=com.segmentengine.cli.DemoCli -Dexec.args="demo --mode benchmark --preset 100k --seed 42 --output benchmark-report.csv"
```

Run HTTP API server:

```bash
mvn -q exec:java -Dexec.mainClass=com.segmentengine.api.ApiServer -Dexec.args="--port 8080"
```

Example API call:

```bash
curl -sS http://localhost:8080/v1/evaluate \
  -H "content-type: application/json" \
  -d @- <<'JSON'
{
  "segments": [
    {"name":"high_value","rule":"age > 25 AND total_spent >= 1000 AND last_login_days < 30"}
  ],
  "profiles": [
    {"id":1,"age":26,"totalSpent":1500,"lastLoginDays":20},
    {"id":2,"age":22,"totalSpent":3000,"lastLoginDays":2}
  ]
}
JSON
```

## CLI Contract

```txt
demo --mode parse --segments <path> [--optimize]
demo --mode evaluate --segments <path> --profiles <path> [--optimize]
demo --mode incremental --segments <path> --profiles <path> --updates <path> [--optimize]
demo --mode benchmark [--preset <50k|100k|500k>] [--profile-count <n>] [--segment-count <n>] [--seed <n>] [--optimize] [--output <path>] [--format <csv|json>]
```

Additional benchmark flags:

- `--preset` (`50k`, `100k`, `500k`)
- `--profile-count`
- `--segment-count`
- `--output` (path ending in `.csv` or `.json`)
- `--format` (`csv` or `json`, optional if extension is present)

## API Endpoints

- `GET /v1/health`
- `POST /v1/parse`
- `POST /v1/evaluate`
- `POST /v1/incremental`
- `POST /v1/benchmark`

Compatibility aliases (non-versioned routes) remain available:

- `GET /health`
- `POST /parse`
- `POST /evaluate`
- `POST /incremental`
- `POST /benchmark`

OpenAPI specification:

- [openapi.yaml](/Users/maruti/Documents/Projects/java-segmentation-engine/openapi.yaml)

## Implementation Prompt

The implementation prompt has been moved to:

- [IMPLEMENTATION_PROMPT.md](/Users/maruti/Documents/Projects/java-segmentation-engine/IMPLEMENTATION_PROMPT.md)
