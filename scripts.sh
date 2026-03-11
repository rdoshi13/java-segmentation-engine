#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ $# -eq 0 ]]; then
  echo "Usage: ./scripts.sh --mode <parse|evaluate|incremental|benchmark> [args...]"
  echo "Example:"
  echo "  ./scripts.sh --mode evaluate --segments src/test/resources/demo/segments.json --profiles src/test/resources/demo/profiles.json"
  exit 1
fi

# Prepare compiled classes and runtime dependencies if needed.
if [[ ! -d target/classes || ! -d target/dependency ]]; then
  mvn -q -DskipTests package dependency:copy-dependencies
fi

java -cp "target/classes:target/dependency/*" com.segmentengine.cli.DemoCli demo "$@"
