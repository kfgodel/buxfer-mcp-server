# Same structure as transaction_add — fields sit directly under .response.
  .response.id = (.response.id % 64999 + 1)
| .response.accountId = (.response.accountId % 64999 + 1)
| .response.description = "Test Transaction (edited)"
| .response.accountName = "Test Account"
