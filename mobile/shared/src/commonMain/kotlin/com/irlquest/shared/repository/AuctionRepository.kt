package com.irlquest.shared.repository

import com.irlquest.shared.models.AuctionItem
import com.irlquest.shared.models.AuctionPurchaseRequest
import com.irlquest.shared.models.AuctionPurchaseResult
import com.irlquest.shared.models.CreateAuctionListingRequest
import com.irlquest.shared.network.ApiClient

class AuctionRepository(
    private val apiClient: ApiClient
) {
    suspend fun getListings(token: String? = null): List<AuctionItem> {
        val headers = token?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        return try {
            apiClient.get("/auction/listings", headers)
        } catch (e: Exception) {
            // Backend endpoint not implemented yet - return empty list
            emptyList()
        }
    }

    suspend fun createListing(request: CreateAuctionListingRequest, token: String): AuctionItem {
        return apiClient.postWithBody(
            path = "/auction/listings",
            body = request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }

    suspend fun buyListing(request: AuctionPurchaseRequest, token: String): AuctionPurchaseResult {
        return apiClient.postWithBody(
            path = "/auction/purchase",
            body = request,
            headers = mapOf("Authorization" to "Bearer $token")
        )
    }
}

