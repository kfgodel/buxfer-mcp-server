# Anonymise group IDs, names, and member names.
.response.groups |= map(
  .id = (.id % 64999 + 1)
  | .name = "Test Group \(.id)"
  | .members = [
      .members | to_entries[] |
      .value.name = "Member \(.key + 1)" |
      .value
    ]
)
