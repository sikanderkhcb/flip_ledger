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
import com.circuitflip.flipledger.domain.model.WorkspaceType
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
    // Read-only: the DB fills created_at (default now()); never send it on insert.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("business_name") val businessName: String = "Circuit Flip Co.",
    @SerialName("owner_name") val ownerName: String = "",
    @SerialName("partner_name") val partnerName: String = "Marcus",
    @SerialName("workspace_type") val workspaceType: String = "partner",
    @SerialName("split_you") val splitYou: Int = 60,
    val currency: String = "USD",
    @SerialName("category_pref") val categoryPref: String = "mixed",
    val onboarded: Boolean = false,
)

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
    daysHeld = daysHeld,
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
