# Limit to 2 reminders. Replace IDs, anonymise name and description.
# Nested tags array and account object are also cleaned up.
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
  | del(.account)
))
