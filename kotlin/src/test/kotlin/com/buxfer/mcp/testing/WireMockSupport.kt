package com.buxfer.mcp.testing

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

/**
 * Builds a WireMockServer that serves the shared `shared/test-fixtures/wiremock/`
 * directory (with `mappings/` and `__files/` subdirs — the canonical WireMock layout).
 * The same directory is mounted by the Docker WireMock in api-recordings/compose.yml,
 * so embedded and Docker tiers share an identical contract.
 *
 * The root path is injected via the `wiremock.root.dir` system property in
 * build.gradle.kts so tests don't hardcode relative paths.
 *
 * Returns the unstarted server; the caller owns lifecycle.
 */
object WireMockSupport {
    fun newServer(): WireMockServer {
        val rootDir = requireNotNull(System.getProperty("wiremock.root.dir")) {
            "wiremock.root.dir system property not set; check build.gradle.kts"
        }
        val config = WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderDirectory(rootDir)
            .notifier(ConsoleNotifier(false))
        return WireMockServer(config)
    }
}
