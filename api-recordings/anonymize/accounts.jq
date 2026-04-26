# Anonymize account names and bank names.
# IDs, balances, and lastSynced are not personal — kept as-is.
.response.accounts |= map(
  .name = "Test Account \(.id)"
  | .bank = "Test Bank"
)
