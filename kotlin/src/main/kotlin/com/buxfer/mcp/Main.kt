package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferApiException
import com.buxfer.mcp.api.BuxferClient
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking

fun main() {
    val email = System.getenv("BUXFER_EMAIL")
    val password = System.getenv("BUXFER_PASSWORD")
    if (email.isNullOrBlank() || password.isNullOrBlank()) {
        System.err.println("BUXFER_EMAIL and BUXFER_PASSWORD must be set")
        exitProcess(1)
    }

    runBlocking {
        val client = BuxferClient()
        try {
            client.login(email, password)
        } catch (e: BuxferApiException) {
            System.err.println("Login failed: ${e.message}")
            exitProcess(1)
        }
        BuxferMcpServer(client).start()
    }
}
