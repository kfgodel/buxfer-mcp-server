# Limit to 2 reminders. Replace IDs, anonymise name and description.
# Nested tags array and account object are anonymised in place — never deleted —
# so the captured fixture preserves the live response shape.
.response.reminders |= (.[0:2] | map(
  .id = (.id % 64999 + 1)
  | .accountId = (.accountId % 64999 + 1)
  | .name = "Reminder \(.id)"
  | .description = "Reminder description \(.id)"
  | .tags |= map(
      .id = (.id % 64999 + 1)
      | .name = "Tag \(.id)"
      | .relativeName = "tag \(.id)"
      | .parentId = (if .parentId != null then (.parentId % 64999 + 1) else null end)
    )
  # Anonymise sensitive nested account values; pass through metadata fields
  # (type/typeName/sectionName/canSync/hasTransactions/status/...) unchanged.
  | .account.id = (.account.id % 64999 + 1)
  | .account.name = "Test Account \(.account.id)"
  | .account.balance = (.account.id % 999 + 0.01)
  | .account.balanceInDefaultCurrency = (.account.id % 999 + 0.01)
  | .account.bank.id = (.account.bank.id % 64999 + 1)
  | .account.bank.name = "Test Bank"
))
