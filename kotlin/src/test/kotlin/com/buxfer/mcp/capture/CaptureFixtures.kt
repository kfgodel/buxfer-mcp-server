package com.buxfer.mcp.capture

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

// Fixture capture utility — hits the real Buxfer API, anonymizes responses,
// and writes them to shared/test-fixtures/responses/.
//
// Tagged "capture" so it is EXCLUDED from normal test runs (see build.gradle.kts).
// Run explicitly when you need to refresh fixtures from a real account:
//
//   export BUXFER_EMAIL="your@email.com"
//   export BUXFER_PASSWORD="yourpassword"
//   gradle test -Dinclude.tags=capture
//
// After running, review the diff in shared/test-fixtures/responses/ carefully
// to ensure no real personal data was captured before committing.
//
// TODO: Implement once BuxferClient is complete.
//
// Implementation outline:
//   1. Read BUXFER_EMAIL and BUXFER_PASSWORD from System.getenv().
//      Fail fast with a clear message if either is missing.
//   2. Construct a real BuxferClient (no mock engine) and call login().
//   3. Call every GET endpoint and collect the raw JSON response strings.
//   4. For write endpoints, run minimal test operations (e.g. add then delete
//      a test transaction) and capture those responses too.
//   5. Pass each response through anonymize() before writing.
//   6. Write each anonymized JSON to the fixtures.dir path (same system
//      property used by tests: ${project.projectDir}/../shared/test-fixtures/responses).
//
// Anonymization rules (see shared/test-fixtures/CLAUDE.md for full table):
//   - Replace real names with "Test User", "Contact One", "Roommate One", etc.
//   - Replace real emails with contact.N@example.com
//   - Replace bank names with "Test Bank" / "Other Test Bank"
//   - Shift all dates to the 2025-01-xx range
//   - Replace IDs with sequential test IDs starting at 1001, 2001, etc.
//   - Round monetary amounts to 2 decimal places with similar magnitude

@Tag("capture")
class CaptureFixtures {

    @Test
    fun `capture and anonymize all Buxfer API responses`() {
        TODO("Not yet implemented — implement after BuxferClient is complete")
    }
}
