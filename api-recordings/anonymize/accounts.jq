# Limit to 5 accounts. Replace IDs with small anonymised values (% 64999 + 1),
# anonymise names and bank, cap balance to < 1000.
.response.accounts |= (.[0:5] | map(
  .id = (.id % 64999 + 1)
  | .name = "Test Account \(.id)"
  | .bank = "Test Bank"
  | .balance = (.id % 999 + 0.01)
))
