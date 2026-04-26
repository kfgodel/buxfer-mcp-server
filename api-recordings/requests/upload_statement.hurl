# upload_statement — manual capture required
#
# This endpoint requires a real bank statement file (CSV, OFX, or QIF format).
# It cannot be captured automatically by run-capture.sh because we don't want
# to import real transactions into your Buxfer account during capture.
#
# To capture manually:
#   1. Prepare a minimal test statement file (e.g. a CSV with 1–2 rows).
#   2. Run:
#        TOKEN=$(hurl requests/login.hurl \
#                  --variable BUXFER_EMAIL=$BUXFER_EMAIL \
#                  --variable BUXFER_PASSWORD=$BUXFER_PASSWORD | jq -r '.token')
#        hurl requests/upload_statement.hurl \
#                  --variable token=$TOKEN \
#                  --variable account_id=<your_account_id> \
#                  --variable statement_text="Date,Description,Amount\n2025-01-01,Test,1.00" \
#          | jq -f anonymize/upload_statement.jq \
#          > ../shared/test-fixtures/responses/upload_statement.json
#   3. Verify and commit.

POST https://www.buxfer.com/api/upload_statement
[QueryStringParams]
token: {{token}}
[FormParams]
accountId: {{account_id}}
statement: {{statement_text}}

HTTP 200
