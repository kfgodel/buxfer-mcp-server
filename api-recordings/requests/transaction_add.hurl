POST https://www.buxfer.com/api/transaction_add
[QueryStringParams]
token: {{token}}
[FormParams]
description: api-recordings capture test
amount: 0.01
accountId: {{account_id}}
date: {{date}}
type: expense
status: cleared

HTTP 200
