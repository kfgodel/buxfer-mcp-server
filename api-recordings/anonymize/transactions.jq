# Anonymize transaction descriptions.
# Type, status, amount, date, and accountId are structural — kept as-is.
# Tags are kept as-is (generic category names are not personal).
.response.transactions |= map(
  .description = "Transaction \(.id)"
)
