# Buxfer REST API Specification

Source: https://www.buxfer.com/help/api

## Base URL

```
https://www.buxfer.com/api/{command}
```

All requests except `login` require a `token` query parameter.

## Authentication

Buxfer uses **ephemeral token-based authentication**.

### Flow

1. POST to `/api/login` with `email` and `password` → receive a `token`.
2. Append `?token={token}` to every subsequent request.
3. Tokens are ephemeral (session-scoped). Re-login when a token expires.

The token mechanism also prevents CSRF. Credentials are transmitted over 128-bit SSL.

---

## Endpoints

### `POST /api/login`

Authenticate and obtain a session token.

> Only reads POST parameters; GET parameters are discarded.

**Request parameters**

| Field    | Type   | Required | Description           |
|----------|--------|----------|-----------------------|
| email    | string | yes      | Buxfer account email  |
| password | string | yes      | Buxfer account password |

**Response**

```json
{
  "status": "OK",
  "token": "<session_token>"
}
```

---

### `GET /api/transactions`

List transactions with optional filters. Returns up to **100 transactions per page**.

**Query parameters**

| Field       | Type   | Required | Description                                              |
|-------------|--------|----------|----------------------------------------------------------|
| accountId   | int    | no       | Filter by account ID                                     |
| accountName | string | no       | Filter by account name (alternative to accountId)        |
| tagId       | int    | no       | Filter by tag ID                                         |
| tagName     | string | no       | Filter by tag name                                       |
| startDate   | string | no       | Start date `YYYY-MM-DD`                                  |
| endDate     | string | no       | End date `YYYY-MM-DD`                                    |
| budgetId    | int    | no       | Filter by budget ID                                      |
| budgetName  | string | no       | Filter by budget name                                    |
| contactId   | int    | no       | Filter by contact ID                                     |
| contactName | string | no       | Filter by contact name                                   |
| groupId     | int    | no       | Filter by group ID                                       |
| groupName   | string | no       | Filter by group name                                     |
| status      | string | no       | `pending`, `reconciled`, or `cleared`                    |
| page        | int    | no       | Page number (1-based, default 1)                         |

**Response**

```json
{
  "status": "OK",
  "transactions": [ /* array of Transaction objects */ ],
  "numTransactions": 42
}
```

---

### `POST /api/transaction_add`

Add a new transaction.

**Request parameters**

| Field       | Type   | Required | Description                                                   |
|-------------|--------|----------|---------------------------------------------------------------|
| description | string | yes      | Transaction description                                       |
| amount      | number | yes      | Transaction amount (positive)                                 |
| accountId   | int    | yes      | Account to post to                                            |
| date        | string | yes      | Date in `YYYY-MM-DD` format                                   |
| tags        | string | no       | Comma-separated tag names                                     |
| type        | string | no       | See Transaction Types below (default: `expense`)              |
| status      | string | no       | `pending`, `cleared`, or `reconciled`                         |

**Shared bill extra fields** (when `type = sharedBill`)

| Field       | Type   | Description                                              |
|-------------|--------|----------------------------------------------------------|
| payers      | JSON   | Array of `{name, amount}` objects                        |
| sharers     | JSON   | Array of `{name, amount}` objects                        |
| isEvenSplit | bool   | Whether cost is split evenly                             |

**Loan extra fields** (when `type = loan`)

| Field      | Type   | Description                            |
|------------|--------|----------------------------------------|
| loanedBy   | string | UID or email of lender                 |
| borrowedBy | string | UID or email of borrower               |

**Paid-for-friend extra fields** (when `type = paidForFriend`)

| Field   | Type   | Description                  |
|---------|--------|------------------------------|
| paidBy  | string | UID or email of payer        |
| paidFor | string | UID or email of beneficiary  |

**Response**

```json
{
  "status": "OK",
  "transaction": { /* Transaction object */ }
}
```

---

### `POST /api/transaction_edit`

Edit an existing transaction.

**Request parameters**: same as `transaction_add`, plus:

| Field | Type | Required | Description          |
|-------|------|----------|----------------------|
| id    | int  | yes      | Transaction ID to edit |

**Response**

```json
{
  "status": "OK",
  "transaction": { /* updated Transaction object */ }
}
```

---

### `POST /api/transaction_delete`

Delete a transaction.

**Request parameters**

| Field | Type | Required | Description           |
|-------|------|----------|-----------------------|
| id    | int  | yes      | Transaction ID to delete |

**Response**

```json
{ "status": "OK" }
```

---

### `POST /api/upload_statement`

Upload a bank statement for an account.

**Request parameters**

| Field      | Type   | Required | Description                                            |
|------------|--------|----------|--------------------------------------------------------|
| accountId  | int    | yes      | Target account                                         |
| statement  | string | yes      | Statement text (CSV/OFX/QIF content)                   |
| dateFormat | string | no       | `MM/DD/YYYY` or `DD/MM/YYYY`                           |

**Response**

```json
{
  "status": "OK",
  "uploaded": 15,
  "balance": 1234.56
}
```

---

### `GET /api/accounts`

List all accounts.

**Response**

```json
{
  "status": "OK",
  "accounts": [
    {
      "id": 123,
      "name": "Checking",
      "bank": "Chase",
      "balance": 1500.00,
      "lastSynced": "2024-01-15"
    }
  ]
}
```

---

### `GET /api/loans`

List outstanding loans.

**Response**

```json
{
  "status": "OK",
  "loans": [
    {
      "entity": "John Doe",
      "type": "contact",
      "balance": 50.00,
      "description": "Lunch money"
    }
  ]
}
```

---

### `GET /api/tags`

List all tags.

**Response**

```json
{
  "status": "OK",
  "tags": [
    {
      "id": 1,
      "name": "Groceries",
      "parentId": null
    }
  ]
}
```

---

### `GET /api/budgets`

List all budgets.

**Response**

```json
{
  "status": "OK",
  "budgets": [
    {
      "id": 1,
      "name": "Food",
      "limit": 500.00,
      "remaining": 120.00,
      "period": "monthly",
      "currentPeriod": "2024-01",
      "tags": ["Groceries", "Restaurants"],
      "keywords": ["food", "supermarket"]
    }
  ]
}
```

---

### `GET /api/reminders`

List all reminders.

**Response**

```json
{
  "status": "OK",
  "reminders": [
    {
      "id": 1,
      "name": "Rent",
      "startDate": "2024-01-01",
      "period": "monthly",
      "amount": 1200.00,
      "accountId": 5
    }
  ]
}
```

---

### `GET /api/groups`

List all groups with member balances.

**Response**

```json
{
  "status": "OK",
  "groups": [
    {
      "id": 1,
      "name": "Roommates",
      "consolidated": false,
      "members": [
        { "name": "Alice", "balance": 25.00 },
        { "name": "Bob", "balance": -25.00 }
      ]
    }
  ]
}
```

---

### `GET /api/contacts`

List all contacts.

**Response**

```json
{
  "status": "OK",
  "contacts": [
    {
      "id": 1,
      "name": "Alice",
      "email": "alice@example.com",
      "balance": 25.00
    }
  ]
}
```

---

## Data Models

### Transaction

| Field       | Type   | Description                              |
|-------------|--------|------------------------------------------|
| id          | int    | Transaction ID                           |
| description | string | Description                              |
| amount      | number | Amount                                   |
| accountId   | int    | Account ID                               |
| date        | string | Date `YYYY-MM-DD`                        |
| tags        | string | Comma-separated tag names                |
| type        | string | Transaction type (see below)             |
| status      | string | `pending`, `cleared`, or `reconciled`    |

### Transaction Types

| Value               | Description                        |
|---------------------|------------------------------------|
| expense             | Regular expense                    |
| income              | Income                             |
| refund              | Refund                             |
| payment             | Payment                            |
| transfer            | Transfer between accounts          |
| investment_buy      | Investment purchase                |
| investment_sell     | Investment sale                    |
| investment_dividend | Dividend received                  |
| capital_gain        | Capital gain                       |
| capital_loss        | Capital loss                       |
| sharedBill          | Shared expense with group/contacts |
| paidForFriend       | Paid on behalf of someone          |
| settlement          | Settlement of a balance            |
| loan                | Loan transaction                   |

### Account

| Field      | Type   | Description              |
|------------|--------|--------------------------|
| id         | int    | Account ID               |
| name       | string | Account name             |
| bank       | string | Bank/institution name    |
| balance    | number | Current balance          |
| lastSynced | string | Last sync date           |

### Tag

| Field    | Type    | Description              |
|----------|---------|--------------------------|
| id       | int     | Tag ID                   |
| name     | string  | Tag name                 |
| parentId | int?    | Parent tag ID (nullable) |

### Budget

| Field         | Type     | Description                  |
|---------------|----------|------------------------------|
| id            | int      | Budget ID                    |
| name          | string   | Budget name                  |
| limit         | number   | Spending limit               |
| remaining     | number   | Remaining amount             |
| period        | string   | `monthly`, `weekly`, etc.    |
| currentPeriod | string   | Current period identifier    |
| tags          | string[] | Associated tag names         |
| keywords      | string[] | Keywords for auto-tagging    |

### Reminder

| Field     | Type   | Description             |
|-----------|--------|-------------------------|
| id        | int    | Reminder ID             |
| name      | string | Reminder name           |
| startDate | string | Start date `YYYY-MM-DD` |
| period    | string | Recurrence period       |
| amount    | number | Reminder amount         |
| accountId | int    | Associated account      |

### Group

| Field       | Type     | Description               |
|-------------|----------|---------------------------|
| id          | int      | Group ID                  |
| name        | string   | Group name                |
| consolidated| bool     | Whether consolidated      |
| members     | Member[] | Members with balances     |

### Member

| Field   | Type   | Description           |
|---------|--------|-----------------------|
| name    | string | Member name           |
| balance | number | Member balance        |

### Contact

| Field   | Type   | Description           |
|---------|--------|-----------------------|
| id      | int    | Contact ID            |
| name    | string | Contact name          |
| email   | string | Contact email         |
| balance | number | Net balance with them |

### Loan

| Field       | Type   | Description                      |
|-------------|--------|----------------------------------|
| entity      | string | Group or contact name            |
| type        | string | `group` or `contact`             |
| balance     | number | Outstanding balance              |
| description | string | Loan description                 |

---

## MCP Tool Mapping

Each Buxfer endpoint maps to one MCP tool. Suggested tool names:

| Buxfer endpoint      | MCP tool name          |
|----------------------|------------------------|
| POST /login          | `buxfer_login`         |
| GET  /transactions   | `buxfer_list_transactions` |
| POST /transaction_add| `buxfer_add_transaction`   |
| POST /transaction_edit| `buxfer_edit_transaction` |
| POST /transaction_delete| `buxfer_delete_transaction` |
| POST /upload_statement| `buxfer_upload_statement` |
| GET  /accounts       | `buxfer_list_accounts` |
| GET  /loans          | `buxfer_list_loans`    |
| GET  /tags           | `buxfer_list_tags`     |
| GET  /budgets        | `buxfer_list_budgets`  |
| GET  /reminders      | `buxfer_list_reminders`|
| GET  /groups         | `buxfer_list_groups`   |
| GET  /contacts       | `buxfer_list_contacts` |

The `buxfer_login` tool should be called automatically by the server on startup (credentials passed via environment variables `BUXFER_EMAIL` and `BUXFER_PASSWORD`), not exposed as a user-facing tool. The token is stored in server state and injected into every subsequent API call.
