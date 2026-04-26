# Transaction fields sit directly under .response. Replace IDs, anonymise description and accountName.
  .response.id = (.response.id % 64999 + 1)
| .response.accountId = (.response.accountId % 64999 + 1)
| .response.description = "Test Transaction"
| .response.accountName = "Test Account"
