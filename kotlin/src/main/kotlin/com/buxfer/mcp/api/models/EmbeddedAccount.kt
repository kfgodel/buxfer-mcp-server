package com.buxfer.mcp.api.models

import kotlinx.serialization.Serializable

/**
 * Drift-detection schema for the rich, full-detail account object that Buxfer
 * inlines inside other responses — currently `/reminders[*].account`. Used by
 * [com.buxfer.mcp.api.BuxferClient.validateSchema]; not held as data anywhere
 * — the data path forwards the raw JSON to Claude.
 *
 * Distinct from two other account-related models in this package:
 * - [Account]: the simpler `/accounts` summary shape (`bank` is a plain
 *   `String`, no `type` / `status` / `hasTransactions` metadata).
 * - [AccountRef]: the minimal `{id, name}` reference used by transfer-type
 *   transactions in `fromAccount` / `toAccount`.
 *
 * Buxfer emits three different account representations depending on context;
 * this class models the richest one.
 *
 * Three nullability tiers (per the convention in `kotlin/CLAUDE.md`):
 * - **Required** (key + non-null value, fixture-always-present): `id`,
 *   `name`, `type`, `typeName`, `sectionName`, `bank`, `canSync`, `balance`,
 *   `balanceInDefaultCurrency`, `hasTransactions`, `status`,
 *   `needsSyncMigration`, `canEditHoldings`, `isInvestmentType`.
 * - **Optional, spec-forward-compat** (`String? = null` — key may be absent
 *   entirely): `currency`. Present on accounts denominated in a foreign
 *   currency, omitted on local-currency accounts (the captured fixture
 *   covers both branches: reminder #1's USD account has `currency`,
 *   reminder #2's ARS account does not).
 */
@Serializable
data class EmbeddedAccount(
    val id: Int,
    val name: String,
    val type: Int,
    val typeName: String,
    val sectionName: String,
    val bank: Bank,
    val canSync: Boolean,
    val balance: Double,
    val balanceInDefaultCurrency: Double,
    val hasTransactions: Boolean,
    val status: String,
    val needsSyncMigration: Boolean,
    val canEditHoldings: Boolean,
    val isInvestmentType: Boolean,
    val currency: String? = null,
)
