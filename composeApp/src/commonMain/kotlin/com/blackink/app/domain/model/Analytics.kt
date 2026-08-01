package com.blackink.app.domain.model

/** Aggregate figures shown on the Dashboard. All money values in cents. */
data class DashboardMetrics(
    val monthNetProfitCents: Long,
    val profitDeltaPercent: Int,      // vs last month, e.g. 18
    val inventoryValueCents: Long,
    val avgMarginFraction: Double,    // 0.0..1.0
    val agingCount: Int,
    val salesThisMonth: Int,
)

data class CategoryCount(val label: String, val count: Int)
data class CategoryBars(val label: String, val bought: Int, val sold: Int)

/** A "needs attention" nudge on the dashboard. */
data class AttentionItem(
    val title: String,
    val subtitle: String,
    val kind: AttentionKind,
    val deviceId: String? = null,
)

enum class AttentionKind { AGING, READY_TO_LIST, MISSING_EVIDENCE }

/** Partner settlement summary. */
data class Settlement(
    val totalProfitCents: Long,
    val owedCents: Long,
    val yourShareCents: Long,
    val partnerShareCents: Long,
    val activity: List<SettlementActivity>,
)

data class SettlementActivity(val label: String, val amountCents: Long)

/** A row in the reports summary. */
data class ReportMetric(val label: String, val displayValue: String)

/** Subscription plan option. */
data class SubscriptionPlan(
    val name: String,
    val price: String,
    val features: List<String>,
    val highlighted: Boolean = false,
)
