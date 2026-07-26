package com.circuitflip.flipledger.data.remote.dto

import com.circuitflip.flipledger.domain.model.AcquisitionSource
import com.circuitflip.flipledger.domain.model.BusinessProfile
import com.circuitflip.flipledger.domain.model.Cost
import com.circuitflip.flipledger.domain.model.CostType
import com.circuitflip.flipledger.domain.model.Currency
import com.circuitflip.flipledger.domain.model.Device
import com.circuitflip.flipledger.domain.model.DeviceCategory
import com.circuitflip.flipledger.domain.model.DeviceCondition
import com.circuitflip.flipledger.domain.model.DeviceStatus
import com.circuitflip.flipledger.domain.model.LockStatus
import com.circuitflip.flipledger.domain.model.PaidBy
import com.circuitflip.flipledger.domain.model.Sale
import com.circuitflip.flipledger.domain.model.SalesChannel
import com.circuitflip.flipledger.domain.model.SubscriptionAccess
import com.circuitflip.flipledger.domain.model.SubscriptionStatus
import com.circuitflip.flipledger.domain.model.SubscriptionTier
import com.circuitflip.flipledger.domain.model.WorkspaceType
import com.circuitflip.flipledger.domain.util.Dates
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the Supabase `public` tables. Column names are snake_case (`@SerialName`);
 * server-managed columns (`user_id`, `created_at`, `updated_at`) are intentionally omitted —
 * the DB fills them via defaults (`auth.uid()` / `now()`), so we neither send nor decode them.
 */

@Serializable
data class DeviceDto(
    val id: String,
    val category: String,
    val model: String,
    val identifier: String = "",
    val condition: String? = null,
    val storage: String = "",
    val lock: String = "—",
    @SerialName("purchase_price_cents") val purchasePriceCents: Long = 0,
    val source: String? = null,
    @SerialName("purchase_date") val purchaseDate: String = "",
    val status: String = "Purchased",
    @SerialName("days_held") val daysHeld: Int = 0,
    @SerialName("repair_issue") val repairIssue: String = "",
    @SerialName("repair_provider") val repairProvider: String = "",
    @SerialName("repair_started_on") val repairStartedOn: String? = null,
    @SerialName("repair_completed_on") val repairCompletedOn: String? = null,
    @SerialName("warranty_provider") val warrantyProvider: String = "",
    @SerialName("warranty_expires_on") val warrantyExpiresOn: String? = null,
)

@Serializable
data class CostDto(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    val type: String,
    @SerialName("amount_cents") val amountCents: Long = 0,
    @SerialName("paid_by") val paidBy: String = "You",
    val date: String = "",
    val note: String = "",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SaleDto(
    val id: String,
    val model: String,
    @SerialName("sold_date") val soldDate: String = "",
    val channel: String? = null,
    @SerialName("revenue_cents") val revenueCents: Long = 0,
    @SerialName("cost_cents") val costCents: Long = 0,
    @SerialName("fees_cents") val feesCents: Long = 0,
    @SerialName("days_held") val daysHeld: Int = 0,
    @SerialName("purchase_price_cents") val purchasePriceCents: Long = 0,
    @SerialName("purchase_date") val purchaseDate: String = "",
    @SerialName("customer_name") val customerName: String = "",
    @SerialName("customer_email") val customerEmail: String = "",
    @SerialName("customer_phone") val customerPhone: String = "",
    @SerialName("customer_address") val customerAddress: String = "",
    // Read-only: the DB fills created_at (default now()); never send it on insert.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("business_name") val businessName: String = "",
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("partner_name") val partnerName: String = "Partner",
    @SerialName("workspace_type") val workspaceType: String = "solo",
    @SerialName("split_you") val splitYou: Int = 60,
    val currency: String = "USD",
    @SerialName("category_pref") val categoryPref: String = "mixed",
    val onboarded: Boolean = false,
)

@Serializable
data class BillingAccountDto(
    @SerialName("lifetime_devices_created") val lifetimeDevicesCreated: Int = 0,
    @SerialName("plan_tier") val planTier: String = "free",
    @SerialName("subscription_status") val subscriptionStatus: String = "free",
    @SerialName("current_period_end") val currentPeriodEnd: String? = null,
    @SerialName("cancel_at_period_end") val cancelAtPeriodEnd: Boolean = false,
)

@Serializable
data class BillingUrlResponse(val url: String)

@Serializable
data class CheckoutSessionRequest(val plan: String)

// ---- DTO → domain --------------------------------------------------------

fun DeviceDto.toDomain(costs: List<Cost>): Device = Device(
    id = id,
    category = DeviceCategory.fromId(category),
    model = model,
    identifier = identifier,
    condition = DeviceCondition.fromLabel(condition),
    storage = storage,
    lock = LockStatus.fromLabel(lock),
    purchasePriceCents = purchasePriceCents,
    source = AcquisitionSource.fromLabel(source),
    purchaseDate = purchaseDate,
    costs = costs,
    status = DeviceStatus.fromLabel(status),
    daysHeld = Dates.daysBetween(purchaseDate) ?: daysHeld,
    repairIssue = repairIssue,
    repairProvider = repairProvider,
    repairStartedOn = repairStartedOn,
    repairCompletedOn = repairCompletedOn,
    warrantyProvider = warrantyProvider,
    warrantyExpiresOn = warrantyExpiresOn,
)

fun CostDto.toDomain(): Cost = Cost(
    id = id,
    type = CostType.fromLabel(type) ?: CostType.OTHER,
    amountCents = amountCents,
    paidBy = PaidBy.fromLabel(paidBy),
    date = date,
    note = note,
)

fun SaleDto.toDomain(): Sale = Sale(
    id = id,
    model = model,
    soldDate = soldDate,
    channel = SalesChannel.fromLabel(channel),
    revenueCents = revenueCents,
    costCents = costCents,
    feesCents = feesCents,
    daysHeld = daysHeld,
    customerName = customerName,
    customerEmail = customerEmail,
    customerPhone = customerPhone,
    customerAddress = customerAddress,
    createdAt = createdAt,
)

fun ProfileDto.toDomain(): BusinessProfile = BusinessProfile(
    businessName = businessName,
    ownerName = ownerName,
    partnerName = partnerName,
    workspaceType = WorkspaceType.fromId(workspaceType),
    splitYou = splitYou,
    currency = Currency.fromCode(currency),
    categoryPref = categoryPref,
)

fun BillingAccountDto.toDomain(): SubscriptionAccess = SubscriptionAccess(
    tier = SubscriptionTier.fromWire(planTier),
    status = SubscriptionStatus.fromWire(subscriptionStatus),
    lifetimeDevicesCreated = lifetimeDevicesCreated,
    currentPeriodEnd = currentPeriodEnd,
    cancelAtPeriodEnd = cancelAtPeriodEnd,
)

// ---- domain → DTO --------------------------------------------------------

fun Device.toDto(): DeviceDto = DeviceDto(
    id = id,
    category = category.id,
    model = model,
    identifier = identifier,
    condition = condition?.label,
    storage = storage,
    lock = lock.label,
    purchasePriceCents = purchasePriceCents,
    source = source?.label,
    purchaseDate = purchaseDate,
    status = status.label,
    daysHeld = daysHeld,
    repairIssue = repairIssue,
    repairProvider = repairProvider,
    repairStartedOn = repairStartedOn,
    repairCompletedOn = repairCompletedOn,
    warrantyProvider = warrantyProvider,
    warrantyExpiresOn = warrantyExpiresOn,
)

fun Cost.toDto(deviceId: String): CostDto = CostDto(
    id = id,
    deviceId = deviceId,
    type = type.label,
    amountCents = amountCents,
    paidBy = paidBy.label,
    date = date,
    note = note,
)

fun Sale.toDto(): SaleDto = SaleDto(
    id = id,
    model = model,
    soldDate = soldDate,
    channel = channel?.label,
    revenueCents = revenueCents,
    costCents = costCents,
    feesCents = feesCents,
    daysHeld = daysHeld,
    purchasePriceCents = purchasePriceCents,
    purchaseDate = purchaseDate,
    customerName = customerName,
    customerEmail = customerEmail,
    customerPhone = customerPhone,
    customerAddress = customerAddress,
)

fun BusinessProfile.toDto(id: String): ProfileDto = ProfileDto(
    id = id,
    businessName = businessName,
    ownerName = ownerName,
    partnerName = partnerName,
    workspaceType = workspaceType.id,
    splitYou = splitYou,
    currency = currency.code,
    categoryPref = categoryPref,
)
