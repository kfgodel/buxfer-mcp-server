# Buxfer REST API Specification

Source: https://www.buxfer.com/help/api

> **Live API divergences are annotated inline.** During end-to-end verification
> (2026-05-08) several endpoints were observed to behave differently from the
> upstream documentation linked above. Each divergence is captured under a
> `> **Live API divergence**` callout in the affected section, with the actual
> wire shape we received and a deep link to the exact upstream subsection it
> contradicts. Implementations should treat the callouts as the ground truth
> and the surrounding spec text as the upstream's stated intent.
>
> **Verify against the raw HTTP response, not against MCP-tool output.** A
> previously-recorded `transaction_delete` divergence here was a false
> positive: the shape we "observed" — `{"deleted": true, "id": <int>}` —
> turned out to be synthesized by `TransactionTools.deleteTransaction` in the
> Kotlin server, not produced by Buxfer. The live API really does return
> `{"status":"OK"}` per the upstream docs and the captured fixture. Anchor
> any future divergence to a raw response (e.g. via
> `api-recordings/run-capture.sh`) — the MCP layer strips the standard
> `response` envelope and may otherwise reshape payloads, so its output is
> downstream of transformations and is not a reliable witness to the wire
> shape.

## Base URL

```
https://www.buxfer.com/api/{command}
```

All requests except `login` require a `token` query parameter.

## Response envelope

Every response is wrapped in a `response` object:

```json
{ "response": { "status": "OK", ...payload } }
```

Implementations must unwrap `.response` before accessing payload fields.

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
  "response": {
    "status": "OK",
    "token": "<session_token>"
  }
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

> **Live API divergence (verified 2026-05-08)** vs [upstream `transactions` section](https://www.buxfer.com/help/api#transactions):
> `numTransactions` is returned as a JSON **string**, not a number. Observed:
> `"numTransactions": "28753"`. Models in this repo (`Transactions.kt`) already
> declare it as `String` per a captured fixture; only this spec document said
> `int`.

---

### `POST /api/transaction_add`

Add a new transaction.

**Request parameters**

| Field         | Type   | Required | Description                                                   |
|---------------|--------|----------|---------------------------------------------------------------|
| description   | string | yes      | Transaction description                                       |
| amount        | number | yes      | Transaction amount (positive)                                 |
| accountId     | int    | no²      | Account to post to (use `fromAccountId`/`toAccountId` for transfers) |
| fromAccountId | int    | no²      | Source account for `type=transfer`                            |
| toAccountId   | int    | no²      | Destination account for `type=transfer`                       |
| date          | string | yes      | Date in `YYYY-MM-DD` format                                   |
| tags          | string | no       | Comma-separated tag names                                     |
| type          | string | yes¹     | See Transaction Types below                                   |
| status        | string | no       | `pending` or `cleared` (³)                                    |

> **Live API divergence (verified 2026-05-08)** vs [upstream `transaction_add` section](https://www.buxfer.com/help/api#transaction_add):
> ¹ `type` is documented as optional (default `expense`) but the live API
> rejects requests without it with `HTTP 400: {"error":{"type":"client","message":"Missing value for parameter [type]"}}`.
> Treat `type` as **required**.

> ² Account routing is conditional, not endpoint-required: `accountId` is needed
> for non-transfer types (`expense`, `income`, `sharedBill`, …); `fromAccountId`
> and `toAccountId` are needed for `type=transfer`. The upstream docs list all
> three as optional because the endpoint accepts whichever pair makes sense for
> the type — but at least one valid pair is required in practice.

> ³ `status` accepts only `pending` and `cleared` at create/edit time per
> the upstream docs; `reconciled` is a state a transaction reaches via
> reconciliation actions, not via direct write. The `/transactions` filter
> does accept `reconciled` as a query value (see that endpoint).

**Shared bill extra fields** (when `type = sharedBill`)

| Field       | Type   | Description                                              |
|-------------|--------|----------------------------------------------------------|
| payers      | JSON   | Array of `{email, amount}` objects                       |
| sharers     | JSON   | Array of `{email, amount}` objects (amount optional when `isEvenSplit=true`) |
| isEvenSplit | bool   | Whether cost is split evenly                             |

> The upstream example uses `email` to identify each payer/sharer (the field
> can also be `name` if the contact has no email on file, but `email` is the
> documented and recommended form). This repository's implementation only
> exposes `email`.

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

> **Live API divergence (verified 2026-05-08)** vs [upstream `transaction_add` section](https://www.buxfer.com/help/api#transaction_add):
> the response is the **bare Transaction object**, not the documented
> `{status, transaction}` envelope. Observed payload (after the standard
> `response` unwrap):
>
> ```json
> {
>   "id": 240160724,
>   "description": "Test from MCP",
>   "date": "2026-05-08",
>   "type": "expense",
>   "transactionType": "expense",
>   "amount": 0.01,
>   "expenseAmount": 0.01,
>   "accountId": 205346,
>   "accountName": "Bolsillo",
>   "tags": "",
>   "tagNames": [],
>   "status": "cleared",
>   "isFutureDated": false,
>   "isPending": false
> }
> ```

---

### `POST /api/transaction_edit`

Edit an existing transaction.

**Request parameters**: same shape as `transaction_add`, plus `id`. Per the
upstream docs, only `id` is strictly required — every other field is documented
as optional, so a partial update should be possible (e.g. change just the
description). **This has not been verified against the live API by this
repository.** The MCP server currently mirrors `transaction_add`'s required
set (`description`, `amount`, `accountId`, `date`, `type`) on the input
schema. If a future smoke test confirms partial updates are accepted, relax
the required list in [TransactionTools.kt][edit-schema].

[edit-schema]: ../../kotlin/src/main/kotlin/com/buxfer/mcp/tools/TransactionTools.kt

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

> **Live API divergence (verified 2026-05-08)** vs [upstream `upload_statement` section](https://www.buxfer.com/help/api#upload_statement):
> upstream documents `uploaded` as a boolean, but the live API returns the
> integer count of transactions imported. The captured fixture (`__files/upload_statement.json`)
> and `UploadStatementResult.kt` already model it as an int.

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
