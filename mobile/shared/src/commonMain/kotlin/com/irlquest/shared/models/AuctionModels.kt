package com.irlquest.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ItemQuality {
    @SerialName("common")
    COMMON,

    @SerialName("uncommon")
    UNCOMMON,

    @SerialName("rare")
    RARE,

    @SerialName("epic")
    EPIC,

    @SerialName("legendary")
    LEGENDARY
}

@Serializable
data class AuctionItem(
    val id: Int,
    val name: String,
    val quality: ItemQuality = ItemQuality.COMMON,
    val price: Int,
    val quantity: Int = 1,
    val seller: String,
    val description: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreateAuctionListingRequest(
    val name: String,
    val quality: ItemQuality,
    val price: Int,
    val quantity: Int = 1,
    val description: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("duration_hours") val durationHours: Int = 24
)

@Serializable
data class AuctionPurchaseRequest(
    @SerialName("listing_id") val listingId: Int,
    val quantity: Int = 1
)

@Serializable
data class AuctionPurchaseResult(
    val success: Boolean,
    val message: String? = null,
    @SerialName("remaining_balance") val remainingBalance: Int? = null
)

