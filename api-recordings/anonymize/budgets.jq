# Limit to 2 budgets. Replace all IDs and anonymise all name attributes.
.response.budgets |= (.[0:2] | map(
  .id = (.id % 64999 + 1)
  | .budgetId = .id
  | .name = "Budget \(.id)"
  | .tagId = (.tagId % 64999 + 1)
  | .eventId = ((.eventId // 0) % 64999 + 1)
  | .tag.id = .tagId
  | .tag.name = "Budget Tag \(.tagId)"
  | .tag.relativeName = "budget tag \(.tagId)"
  | .tag.parentId = (if .tag.parentId != null then (.tag.parentId % 64999 + 1) else null end)
))
