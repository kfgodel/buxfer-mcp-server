package com.buxfer.mcp

import java.io.File

object TestFixtureLoader {

    private val fixturesDir: File by lazy {
        val path = System.getProperty("fixtures.dir")
            ?: error("fixtures.dir system property not set — run tests via Gradle, not directly")
        File(path).also {
            require(it.isDirectory) { "fixtures.dir does not exist or is not a directory: $path" }
        }
    }

    fun load(name: String): String =
        fixturesDir.resolve("$name.json").readText()
}
