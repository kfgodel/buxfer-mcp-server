# Anonymize contact names and emails.
# Balances are kept as-is.
.response.contacts = [
  .response.contacts | to_entries[] |
  .value.name  = "Contact \(.key + 1)" |
  .value.email = "contact.\(.key + 1)@example.com" |
  .value
]
