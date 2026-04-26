# Anonymize contact names and emails.
# Balances are kept as-is.
.contacts = [
  .contacts | to_entries[] |
  .value.name  = "Contact \(.key + 1)" |
  .value.email = "contact.\(.key + 1)@example.com" |
  .value
]
