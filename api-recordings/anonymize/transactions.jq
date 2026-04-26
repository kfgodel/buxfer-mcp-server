# Limit to 5 transactions. Replace IDs, anonymise descriptions, account names, and tag names.
.response.transactions |= (.[0:5] | map(
  .id = (.id % 64999 + 1)
  | .accountId = (if .accountId != null then (.accountId % 64999 + 1) else null end)
  | .description = "Transaction \(.id)"
  | .accountName = "Test Account"
  | .tags = (if .tags != "" then "Tag 1" else "" end)
  | .tagNames = (.tagNames | to_entries | map("Tag \(.key + 1)"))
))
| .response.numTransactions = "5"
