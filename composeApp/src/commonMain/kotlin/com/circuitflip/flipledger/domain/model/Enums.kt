package com.circuitflip.flipledger.domain.model

/**
 * All fixed domain enumerations for FlipLedger. Each carries a stable [id] used for
 * persistence/serialization and a human [label] used in the UI. Chip lists in the
 * screens are derived directly from [entries], so adding a value is a one-line change.
 */

/** Product category of a resale device. */
enum class DeviceCategory(val id: String, val label: String) {
    PHONE("phone", "Phone"),
    LAPTOP("laptop", "Laptop"),
    TABLET("tablet", "Tablet"),
    GAMING("gaming", "Gaming console"),
    ACCESSORY("accessory", "Accessory");

    companion object {
        fun fromId(id: String): DeviceCategory = entries.firstOrNull { it.id == id } ?: PHONE
    }
}

/** Physical condition grade recorded when adding a device. */
enum class DeviceCondition(val label: String) {
    LIKE_NEW("Like New"),
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    FOR_PARTS("For Parts");

    companion object {
        fun fromLabel(label: String?): DeviceCondition? = entries.firstOrNull { it.label == label }
    }
}

/** Carrier-lock status (phones/tablets). */
enum class LockStatus(val label: String) {
    UNLOCKED("Unlocked"),
    LOCKED("Locked"),
    NONE("—");

    companion object {
        fun fromLabel(label: String?): LockStatus = entries.firstOrNull { it.label == label } ?: NONE
    }
}

/** Where a device was acquired. */
enum class AcquisitionSource(val label: String) {
    FACEBOOK("Facebook Marketplace"),
    EBAY("eBay"),
    LOCAL("Local"),
    AUCTION("Auction"),
    TRADE_IN("Trade-in"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String?): AcquisitionSource? = entries.firstOrNull { it.label == label }
    }
}

/** Lifecycle status of a device in inventory. Drives the status pill color. */
enum class DeviceStatus(val label: String) {
    PURCHASED("Purchased"),
    REPAIR("Repair"),
    READY("Ready"),
    LISTED("Listed"),
    SOLD("Sold");

    companion object {
        fun fromLabel(label: String): DeviceStatus = entries.firstOrNull { it.label == label } ?: PURCHASED
    }
}

/** Category of an additional cost logged against a device. */
enum class CostType(val label: String) {
    PARTS("Parts"),
    LABOR("Labor"),
    SHIPPING("Shipping"),
    PACKAGING("Packaging"),
    PLATFORM_FEE("Platform fee"),
    TAX("Tax"),
    PAYMENT_PROCESSING("Payment processing"),
    ADVERTISING("Advertising"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String?): CostType? = entries.firstOrNull { it.label == label }
    }
}

/** Who paid for a cost (relevant in partner mode). */
enum class PaidBy(val label: String) {
    YOU("You"),
    PARTNER("Partner");

    companion object {
        fun fromLabel(label: String?): PaidBy = entries.firstOrNull { it.label == label } ?: YOU
    }
}

/** Marketplace / channel a sale went through. */
enum class SalesChannel(val label: String) {
    EBAY("eBay"),
    SWAPPA("Swappa"),
    FACEBOOK("Facebook Marketplace"),
    IN_PERSON("In-person"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String?): SalesChannel? = entries.firstOrNull { it.label == label }
    }
}

/** Workspace mode chosen during onboarding. */
enum class WorkspaceType(val id: String) {
    SOLO("solo"),
    PARTNER("partner");

    companion object {
        fun fromId(id: String): WorkspaceType = entries.firstOrNull { it.id == id } ?: PARTNER
    }
}

/** Supported currencies. */
enum class Currency(val code: String, val symbol: String) {
    USD("USD", "$"),
    CAD("CAD", "$");

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.code == code } ?: USD
    }
}
