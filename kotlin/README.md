# Buxfer MCP Server — Kotlin

A [Model Context Protocol](https://modelcontextprotocol.io) server that lets
[Claude Code](https://docs.claude.com/en/docs/claude-code) and
[Claude Desktop](https://claude.ai/download) talk to your
[Buxfer](https://www.buxfer.com) account. This is the Kotlin/JVM
implementation. Once it's wired up you can ask Claude things like
*"list my Buxfer accounts"*, *"how much did I spend on groceries last
month?"*, or *"add a $4 expense for lunch to my Visa"*.

## Prerequisites

- A Buxfer account (email + password)
- Either Claude Code or Claude Desktop installed
- [ASDF](https://asdf-vm.com/) (we'll use it to install Java + Gradle)

## Setup

These are five one-time steps. After that, every new chat with Claude
just works — no further setup until you `git pull` (see below).

### 1. Install Java 21 and Gradle 9.4.1

```bash
cd kotlin
asdf plugin add java     # if not already added
asdf plugin add gradle   # if not already added
asdf install             # installs the versions pinned in .tool-versions
cd ..
```

### 2. Add your Buxfer credentials

```bash
cp .env.example .env
```

Open `.env` and fill in:

```
BUXFER_EMAIL=your@email.com
BUXFER_PASSWORD=yourpassword
```

`.env` is gitignored — credentials never reach the repo.

### 3. Build the fat JAR

```bash
cd kotlin
PATH="$HOME/.asdf/shims:$PATH" gradle shadowJar
```

Output: `kotlin/build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar`.

### 4. Register the MCP with Claude

You need two absolute paths. Run these and keep the values handy:

```bash
# Java install path (from your asdf install)
ls ~/.asdf/installs/java/
# e.g. temurin-21.0.10+7.0.LTS
# → /Users/<you>/.asdf/installs/java/temurin-21.0.10+7.0.LTS/bin/java

# Repo root
pwd
# e.g. /Users/<you>/git/buxfer-mcp-server
```

Then pick **one** of the two integrations.

#### Option A — Claude Code

```bash
claude mcp add buxfer --scope local -- \
  /Users/<you>/.asdf/installs/java/temurin-21.0.10+7.0.LTS/bin/java \
  -jar /Users/<you>/git/buxfer-mcp-server/kotlin/build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar \
  --env-file=/Users/<you>/git/buxfer-mcp-server/.env
```

Verify:

```bash
claude mcp list
# buxfer: ... - ✓ Connected
```

#### Option B — Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`
(macOS) — or the equivalent on your platform — and add:

```json
{
  "mcpServers": {
    "buxfer": {
      "command": "/Users/<you>/.asdf/installs/java/temurin-21.0.10+7.0.LTS/bin/java",
      "args": [
        "-jar",
        "/Users/<you>/git/buxfer-mcp-server/kotlin/build/libs/buxfer-mcp-server-1.0-SNAPSHOT-all.jar",
        "--env-file=/Users/<you>/git/buxfer-mcp-server/.env"
      ]
    }
  }
}
```

Restart Claude Desktop after saving.

> Why all the absolute paths? Claude launches the JVM with no guarantees
> about `PATH` or working directory. Bare `java` typically resolves to
> the ASDF shim, which needs a `.tool-versions` in cwd; using the
> absolute JDK path bypasses that. Same reasoning for the JAR and the
> `--env-file=` value.

### 5. Try it

Start a fresh Claude Code or Claude Desktop session and ask:

> *"List my Buxfer accounts."*

Claude should call `buxfer_list_accounts` and answer with your account
list. From there, all 12 tools are fair game — see [Available tools](#available-tools).

## Available tools

12 MCP tools, one-to-one with the [Buxfer REST API](../shared/api-spec/buxfer-api.md):

- **Accounts:** `buxfer_list_accounts`
- **Transactions:** `buxfer_list_transactions`, `buxfer_add_transaction`,
  `buxfer_edit_transaction`, `buxfer_delete_transaction`,
  `buxfer_upload_statement`
- **Lookups:** `buxfer_list_tags`, `buxfer_list_budgets`,
  `buxfer_list_reminders`, `buxfer_list_groups`,
  `buxfer_list_contacts`, `buxfer_list_loans`

## Updating after a `git pull`

```bash
cd kotlin
PATH="$HOME/.asdf/shims:$PATH" gradle shadowJar
```

Then start a new Claude Code session, or restart Claude Desktop — the
new JAR loads on the next process spawn.

## Troubleshooting

### `claude mcp list` reports "Failed to connect"

Check the server log:

```bash
tail kotlin/logs/server.log
```

Common causes:

- **Missing or wrong `.env` credentials** — login fails. The first log
  line shows where the server tried to load `.env` from; if it's not
  the file you expect, the `--env-file=` path in the registration is
  wrong (must be absolute).
- **Bare `java` in the registration** — ASDF shims need a
  `.tool-versions` in the cwd to resolve, which Claude doesn't provide.
  Use the absolute path to the installed JDK.

### Where do logs land?

`<JVM-cwd>/logs/server.log`. The JVM cwd is whichever directory Claude
launched the process from, which is unpredictable. To pin a stable
location, add to your `.env`:

```
BUXFER_LOG_DIR=/Users/<you>/git/buxfer-mcp-server/kotlin/logs
```

## Where to look next

- [`CLAUDE.md`](CLAUDE.md) — architectural conventions, SDK gotchas,
  schema and validation rules. Read this if you're modifying the
  server's code.
- [`../shared/api-spec/buxfer-api.md`](../shared/api-spec/buxfer-api.md)
  — the Buxfer REST API contract this server implements, annotated
  inline wherever the live API diverges from the upstream docs.
