#!/usr/bin/env bash
set -euo pipefail

# (Re)generate WireMock stub mappings from the fixture list.
# Run this whenever a new endpoint is added.
#
# Output: ../shared/test-fixtures/wiremock/mappings/*.json

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAPPINGS_DIR="$SCRIPT_DIR/../shared/test-fixtures/wiremock/mappings"

mkdir -p "$MAPPINGS_DIR"

# ---------------------------------------------------------------------------
# login — always returns a fixed mock token; no fixture file needed
# ---------------------------------------------------------------------------
cat > "$MAPPINGS_DIR/login.json" << 'EOF'
{
  "request": {
    "method": "POST",
    "urlPathEqualTo": "/api/login"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "body": "{\"status\":\"OK\",\"token\":\"test-mock-token\"}"
  }
}
EOF
echo "  ✓ login.json"

# ---------------------------------------------------------------------------
# GET endpoints
# ---------------------------------------------------------------------------
for name in accounts transactions tags budgets reminders groups contacts loans; do
  cat > "$MAPPINGS_DIR/$name.json" << EOF
{
  "request": {
    "method": "GET",
    "urlPathEqualTo": "/api/$name"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "bodyFileName": "$name.json"
  }
}
EOF
  echo "  ✓ $name.json"
done

# ---------------------------------------------------------------------------
# POST endpoints
# ---------------------------------------------------------------------------
for name in transaction_add transaction_edit transaction_delete upload_statement; do
  cat > "$MAPPINGS_DIR/$name.json" << EOF
{
  "request": {
    "method": "POST",
    "urlPathEqualTo": "/api/$name"
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "bodyFileName": "$name.json"
  }
}
EOF
  echo "  ✓ $name.json"
done

echo ""
echo "✓ WireMock mappings written to $MAPPINGS_DIR"
