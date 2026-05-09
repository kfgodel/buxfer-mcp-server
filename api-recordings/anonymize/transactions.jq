# Limit to 5 transactions. Replace IDs, anonymise descriptions, account names, and tag names.
# accountId / accountName are guarded with `has(...)` because transfer-type
# records do NOT carry those keys (they use fromAccount / toAccount instead).
# Without the guard, the anonymizer would fabricate the keys onto transfers
# and the fixture would no longer reflect the live response shape.
# fromAccount / toAccount appear only on transfer-type records — also guarded
# with `has(...)` so the absence is preserved on non-transfer records, and
# their nested name/id are anonymised in place to prevent leaks of real
# account names.
.response.transactions |= (.[0:5] | map(
  .id = (.id % 64999 + 1)
  | .description = "Transaction \(.id)"
  | (if has("accountId") then .accountId = (if .accountId != null then (.accountId % 64999 + 1) else null end) else . end)
  | (if has("accountName") then .accountName = "Test Account" else . end)
  | (if has("fromAccount") then
        .fromAccount.id = (.fromAccount.id % 64999 + 1)
        | .fromAccount.name = "Test Account \(.fromAccount.id)"
      else . end)
  | (if has("toAccount") then
        .toAccount.id = (.toAccount.id % 64999 + 1)
        | .toAccount.name = "Test Account \(.toAccount.id)"
      else . end)
  | .tags = (if .tags != "" then "Tag 1" else "" end)
  | .tagNames = (.tagNames | to_entries | map("Tag \(.key + 1)"))
))
| .response.numTransactions = "5"
