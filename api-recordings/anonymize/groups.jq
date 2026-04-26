# Anonymize group names and member names.
# Balances are kept as-is (not identifiable without knowing who they belong to).
.response.groups |= map(
  .name = "Test Group \(.id)"
  | .members = [
      .members | to_entries[] |
      .value.name = "Member \(.key + 1)" |
      .value
    ]
)
