#!/usr/bin/env bash
set -euo pipefail

# Capture real Buxfer API responses, anonymize them, and write to
# ../shared/test-fixtures/responses/.
#
# Usage:
#   ./run-capture.sh
#
# Credentials are loaded automatically from ../.env (repo root).
# Copy ../.env.example to ../.env and fill in your values before running.
# You can also override by exporting variables before calling this script.
#
# Requires: hurl, jq (both managed via ASDF — run `asdf install` first)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_ENV="$SCRIPT_DIR/../.env"

# Load root .env if present; already-exported variables take precedence
if [ -f "$ROOT_ENV" ]; then
  set -a
  # shellcheck source=/dev/null
  source "$ROOT_ENV"
  set +a
fi

: "${BUXFER_EMAIL:?BUXFER_EMAIL not set — copy .env.example to .env and fill in your credentials}"
: "${BUXFER_PASSWORD:?BUXFER_PASSWORD not set — copy .env.example to .env and fill in your credentials}"

FIXTURES_DIR="$SCRIPT_DIR/../shared/test-fixtures/responses"
REQUESTS_DIR="$SCRIPT_DIR/requests"
ANONYMIZE_DIR="$SCRIPT_DIR/anonymize"

mkdir -p "$FIXTURES_DIR"

# ---------------------------------------------------------------------------
# Login — capture session token
# ---------------------------------------------------------------------------
echo "→ Logging in as $BUXFER_EMAIL ..."
LOGIN_RESPONSE=$(hurl "$REQUESTS_DIR/login.hurl" \
  --variable "BUXFER_EMAIL=$BUXFER_EMAIL" \
  --variable "BUXFER_PASSWORD=$BUXFER_PASSWORD") \
  || { echo "✗ login request failed" >&2; exit 1; }

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.response.token')
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "✗ Login failed. Response: $LOGIN_RESPONSE" >&2
  exit 1
fi
echo "  Token acquired."

echo "$LOGIN_RESPONSE" \
  | jq -f "$ANONYMIZE_DIR/login.jq" \
  > "$FIXTURES_DIR/login.json"
echo "  ✓ $FIXTURES_DIR/login.json"

# ---------------------------------------------------------------------------
# Helper: capture a GET endpoint and anonymize
# ---------------------------------------------------------------------------
capture_get() {
  local name=$1
  echo "→ Capturing $name ..."
  hurl "$REQUESTS_DIR/$name.hurl" \
    --variable "token=$TOKEN" \
    | jq -f "$ANONYMIZE_DIR/$name.jq" \
    > "$FIXTURES_DIR/$name.json"
  echo "  ✓ $FIXTURES_DIR/$name.json"
}

# ---------------------------------------------------------------------------
# GET endpoints
# ---------------------------------------------------------------------------
echo "→ Capturing accounts ..."
ACCOUNTS_RESPONSE=$(hurl "$REQUESTS_DIR/accounts.hurl" --variable "token=$TOKEN") \
  || { echo "✗ accounts request failed" >&2; exit 1; }

# Extract the real account ID BEFORE anonymisation (anonymised IDs are not valid Buxfer IDs)
ACCOUNT_ID=$(echo "$ACCOUNTS_RESPONSE" | jq '.response.accounts[0].id')
echo "  Using account ID $ACCOUNT_ID for write operations."

echo "$ACCOUNTS_RESPONSE" \
  | jq -f "$ANONYMIZE_DIR/accounts.jq" \
  > "$FIXTURES_DIR/accounts.json"
echo "  ✓ $FIXTURES_DIR/accounts.json"

capture_get transactions
capture_get tags
capture_get budgets
capture_get reminders
capture_get groups
capture_get contacts
capture_get loans

# ---------------------------------------------------------------------------
# Write endpoints — add a test transaction, edit it, then delete it
# ---------------------------------------------------------------------------
TODAY=$(date +%Y-%m-%d)
echo "→ Capturing transaction write operations (account $ACCOUNT_ID, date $TODAY) ..."

ADD_RESPONSE=$(hurl "$REQUESTS_DIR/transaction_add.hurl" \
  --variable "token=$TOKEN" \
  --variable "account_id=$ACCOUNT_ID" \
  --variable "date=$TODAY") \
  || { echo "✗ transaction_add request failed" >&2; exit 1; }
echo "$ADD_RESPONSE" \
  | jq -f "$ANONYMIZE_DIR/transaction_add.jq" \
  > "$FIXTURES_DIR/transaction_add.json"
echo "  ✓ $FIXTURES_DIR/transaction_add.json"

TRANSACTION_ID=$(echo "$ADD_RESPONSE" | jq '.response.id')

hurl "$REQUESTS_DIR/transaction_edit.hurl" \
  --variable "token=$TOKEN" \
  --variable "transaction_id=$TRANSACTION_ID" \
  | jq -f "$ANONYMIZE_DIR/transaction_edit.jq" \
  > "$FIXTURES_DIR/transaction_edit.json"
echo "  ✓ $FIXTURES_DIR/transaction_edit.json"

hurl "$REQUESTS_DIR/transaction_delete.hurl" \
  --variable "token=$TOKEN" \
  --variable "transaction_id=$TRANSACTION_ID" \
  | jq -f "$ANONYMIZE_DIR/transaction_delete.jq" \
  > "$FIXTURES_DIR/transaction_delete.json"
echo "  ✓ $FIXTURES_DIR/transaction_delete.json"

# ---------------------------------------------------------------------------
# upload_statement — requires a real statement file; skipped by default
# ---------------------------------------------------------------------------
echo ""
echo "⚠  upload_statement was NOT captured automatically."
echo "   See requests/upload_statement.hurl for manual capture instructions."

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
echo ""
echo "✓ Capture complete. Review the diff before committing:"
echo "  git diff ../shared/test-fixtures/responses/"
echo ""
echo "  Ensure no real names, emails, account details, or bank names appear."
