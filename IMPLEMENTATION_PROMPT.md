# Additional Engineering Constraints

Follow these architectural constraints while implementing the segmentation engine.

## Core Design Principles

- The goal is clean, modular code demonstrating query‑engine concepts.
- Favor readability, determinism, and testability over premature micro‑optimizations.
- The core engine must remain independent of CLI code. The CLI layer should only orchestrate engine components.
- Each slice must maintain the invariant: `mvn test` passes and `demo` runs successfully.

---

# AST Design

## Immutable AST Nodes

- All AST node classes must be immutable value objects.
- Fields should be `final` and set only through constructors.
- Optimizer rewrites must produce new AST nodes instead of mutating existing ones.

This avoids subtle bugs during rewrite passes.

---

## Visitor Pattern for AST Traversal

AST operations must use a visitor pattern rather than `instanceof` chains.

Implement visitors for:

- Expression evaluation
- Field dependency extraction
- Optimizer rewrites
- Pretty printing

---

# Field Access Strategy

Do not use reflection for accessing profile fields.

Instead use a deterministic accessor registry:

```
Map<String, Function<Profile, Object>>
```

This keeps evaluation predictable and avoids reflection overhead.

---

# Dependency Tracking

Incremental evaluation requires knowing which segments depend on which fields.

Implement a utility that extracts referenced fields from an AST.

The output should map:

```
segment → referenced fields
```

This will drive the incremental evaluation step.

---

# Optimizer Architecture

The optimizer must be implemented as a pipeline of rewrite rules, not a single monolithic optimizer.

Define a rewrite interface:

```
RewriteRule
```

The optimizer applies rules sequentially to the AST until no more rewrites occur.

Planned rules include:

- constant folding
- duplicate predicate elimination
- comparison simplification
- predicate ordering by selectivity

---

# Data Model Constraints

Profile objects must be primitive-heavy to reduce memory usage during benchmarking.

Use fields such as:

```
age (int)
totalSpent (double)
lastLoginDays (int)
```

Avoid strings or nested objects in the benchmark dataset.

---

# Segment Membership Representation

Segment membership should be stored as:

```
Map<String, Set<Long>>
```

Segments map to profile IDs, not profile objects.

This more closely resembles real query engines.

---

# Synthetic Data Generation

The synthetic dataset generator must be seeded to ensure deterministic runs.

The seed should be passed through CLI flags.

This guarantees reproducible benchmark results.

---

# Logging

Use SLF4J logging for internal engine diagnostics.

CLI output should only display final user-facing results.

Avoid raw `System.out.println` inside core engine code.

---

# Package Organization

The repository should separate responsibilities clearly:

```
dsl/
engine/
optimizer/
incremental/
model/
benchmark/
cli/
```

Benchmark logic must remain outside the core engine.

---

# AST Optimization Scope

All query optimizations should operate directly on the AST, not through a separate logical-plan layer.

This keeps the implementation simpler for the prototype.

---

# Pretty Printer

Implement an AST pretty-printer utility.

This will be used to display:

- parsed rules
- optimized rules
- debugging output

---

# Final Instruction

The engine should demonstrate concepts used in query engines, rule evaluation systems, and incremental data processing.

Focus on:

- correctness
- deterministic behavior
- modular architecture
- clear test coverage
