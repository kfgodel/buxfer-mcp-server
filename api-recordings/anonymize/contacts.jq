# Limit to 4 contacts. Anonymise IDs, names, and emails. Balances kept as-is.
.response.contacts = [
  .response.contacts[0:4] | to_entries[] |
  .value.id    = (.value.id % 64999 + 1) |
  .value.name  = "Contact \(.key + 1)" |
  .value.email = "contact.\(.key + 1)@example.com" |
  .value
]
