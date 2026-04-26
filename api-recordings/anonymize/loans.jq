# Anonymize loan entity names and descriptions.
# Type ("contact" or "group") and balance are structural — kept as-is.
.response.loans |= [
  to_entries[] |
  .value.entity      = "Test Entity \(.key + 1)" |
  .value.description = "Loan \(.key + 1)" |
  .value
]
