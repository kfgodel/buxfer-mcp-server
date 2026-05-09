# Limit to 4 contacts. Anonymise IDs, names, and emails. Balances kept as-is.
# Email is preserved as null when the source has null, so the captured fixture
# accurately reflects that some real contacts have no email on record.
.response.contacts = [
  .response.contacts[0:4] | to_entries[] |
  .value.id    = (.value.id % 64999 + 1) |
  .value.name  = "Contact \(.key + 1)" |
  .value.email = (if .value.email != null then "contact.\(.key + 1)@example.com" else null end) |
  .value
]
