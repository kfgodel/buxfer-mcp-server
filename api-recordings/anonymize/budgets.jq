# Limit to 2 budgets. Replace all IDs and anonymise all name attributes.
# eventId is preserved as null when the source has null (rather than coerced
# via `// 0`) so the captured fixture keeps the live response's nullity.
.response.budgets |= (.[0:2] | map(
  .id = (.id % 64999 + 1)
  | .budgetId = .id
  | .name = "Budget \(.id)"
  | .tagId = (.tagId % 64999 + 1)
  | .eventId = (if .eventId != null then (.eventId % 64999 + 1) else null end)
  | .tag.id = .tagId
  | .tag.name = "Budget Tag \(.tagId)"
  | .tag.relativeName = "budget tag \(.tagId)"
  | .tag.parentId = (if .tag.parentId != null then (.tag.parentId % 64999 + 1) else null end)
))
