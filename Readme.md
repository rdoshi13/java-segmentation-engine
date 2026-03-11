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
  benchmark/
  metrics/
```

## Run

Run tests:

```bash
mvn test
```

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

## CLI Contract

```txt
demo --mode <parse|evaluate|incremental|benchmark> --segments <path> --profiles <path> [--updates <path>] [--optimize] [--seed <n>]
```

Additional benchmark flags:

- `--profile-count`
- `--segment-count`

## Implementation Prompt

The implementation prompt has been moved to:

- [IMPLEMENTATION_PROMPT.md](/Users/maruti/Documents/Projects/java-segmentation-engine/IMPLEMENTATION_PROMPT.md)
