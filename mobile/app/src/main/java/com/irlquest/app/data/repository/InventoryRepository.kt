package com.irlquest.app.data.repository

import com.irlquest.app.TokenStorage
import com.irlquest.app.data.SharedRepositoryProvider
import com.irlquest.shared.models.AuctionPurchaseResult
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.shared.models.AuctionItem
import com.irlquest.shared.models.AuctionPurchaseRequest
import com.irlquest.shared.models.CreateAuctionListingRequest
import com.irlquest.shared.models.ItemQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.random.Random

data class OwnedItem(
    val localId: Long,
    val name: String,
    val description: String? = null,
    val quality: ItemQuality,
    val basePrice: Int,
    var quantity: Int = 1,
    val icon: String,
    val isEquipped: Boolean = false,
    val obtainedFromQuestId: Int? = null
)

class InventoryRepository(
    private val authViewModel: AuthViewModel? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val auctionRepository = SharedRepositoryProvider.auctionRepository
    private val itemsMutex = sharedMutex
    val items: StateFlow<List<OwnedItem>> = sharedItems

    fun addLootForQuest(
        questId: Int?,
        difficulty: Int,
        playerLuck: Int
    ): List<OwnedItem> {
        val generated = LootGenerator.generateLoot(difficulty, playerLuck)
        if (generated.isEmpty()) return emptyList()

        scope.launch {
            itemsMutex.withLock {
                val updated = sharedItems.value.toMutableList()
                generated.forEach { drop ->
                    val existing = updated.firstOrNull { it.name == drop.name && it.quality == drop.quality }
                    if (existing != null) {
                        val index = updated.indexOf(existing)
                        updated[index] = existing.copy(quantity = existing.quantity + drop.quantity)
                    } else {
                        updated += drop.copy(localId = nextLocalId(), obtainedFromQuestId = questId)
                    }
                }
                sharedItems.value = updated
            }
        }
        return generated
    }

    suspend fun consumeItem(localId: Long, quantity: Int = 1) {
        itemsMutex.withLock {
            val updated = sharedItems.value.toMutableList()
            val index = updated.indexOfFirst { it.localId == localId }
            if (index >= 0) {
                val item = updated[index]
                val newQuantity = item.quantity - quantity
                if (newQuantity <= 0) {
                    updated.removeAt(index)
                } else {
                    updated[index] = item.copy(quantity = newQuantity)
                }
                sharedItems.value = updated
            }
        }
    }

    fun equipItem(localId: Long, equip: Boolean) {
        scope.launch {
            itemsMutex.withLock {
                val updated = sharedItems.value.toMutableList()
                val index = updated.indexOfFirst { it.localId == localId }
                if (index >= 0) {
                    val item = updated[index]
                    updated[index] = item.copy(isEquipped = equip)
                    sharedItems.value = updated
                }
            }
        }
    }

    suspend fun listItemOnAuction(
        localId: Long,
        price: Int,
        quantity: Int = 1,
        thumbnailUrl: String? = null
    ): AuctionItem? {
        val token = TokenStorage.getToken() ?: return null
        val item = itemsMutex.withLock {
            sharedItems.value.firstOrNull { it.localId == localId }
        } ?: return null

        return try {
            val listing = auctionRepository.createListing(
                request = CreateAuctionListingRequest(
                    name = item.name,
                    quality = item.quality,
                    price = price,
                    quantity = quantity,
                    description = item.description,
                    thumbnailUrl = thumbnailUrl
                ),
                token = token
            )
            consumeItem(localId, quantity)
            listing
        } catch (e: Exception) {
            Timber.e(e, "InventoryRepository: failed to list item on auction")
            null
        }
    }

    suspend fun buyFromAuction(listingId: Int, quantity: Int = 1): AuctionPurchaseResult? {
        val token = TokenStorage.getToken() ?: return null
        return try {
            auctionRepository.buyListing(
                request = AuctionPurchaseRequest(listingId = listingId, quantity = quantity),
                token = token
            )
        } catch (e: Exception) {
            Timber.e(e, "InventoryRepository: failed to buy item from auction")
            null
        }
    }

    fun clear() {
        scope.launch {
            itemsMutex.withLock {
                sharedItems.value = emptyList()
            }
        }
    }

    private fun nextLocalId(): Long = Random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE)

    companion object {
        private val sharedMutex = Mutex()
        private val sharedItems = MutableStateFlow<List<OwnedItem>>(emptyList())
    }
}

private object LootGenerator {
    private val weaponNames = listOf(
        "Меч странника",
        "Посох тайных ветров",
        "Изогнутый кинжал",
        "Боевой топор северян",
        "Лук ночного охотника"
    )
    private val armorNames = listOf(
        "Кираса стража",
        "Кольчуга искателя",
        "Плащ теней",
        "Щит хранителя",
        "Маска мудреца"
    )
    private val trinketNames = listOf(
        "Руна вдохновения",
        "Камень портала",
        "Сфера удачи",
        "Свиток прозрения",
        "Фляга духа"
    )

    fun generateLoot(difficulty: Int, playerLuck: Int): List<OwnedItem> {
        val drops = mutableListOf<OwnedItem>()
        val rollCount = 1 + (difficulty / 3)
        repeat(rollCount) {
            val quality = rollQuality(difficulty, playerLuck)
            val name = pickNameForQuality(quality)
            val price = when (quality) {
                ItemQuality.COMMON -> 25
                ItemQuality.UNCOMMON -> 60
                ItemQuality.RARE -> 120
                ItemQuality.EPIC -> 260
                ItemQuality.LEGENDARY -> 520
            }
            val icon = when (quality) {
                ItemQuality.COMMON -> "\uD83D\uDDBC"
                ItemQuality.UNCOMMON -> "\uD83D\uDD2E"
                ItemQuality.RARE -> "\uD83D\uDD2D"
                ItemQuality.EPIC -> "\uD83D\uDC8E"
                ItemQuality.LEGENDARY -> "\uD83D\uDE85"
            }

            if (Random.nextFloat() < dropChanceForQuality(quality, difficulty, playerLuck)) {
                drops += OwnedItem(
                    localId = 0L,
                    name = name,
                    description = descriptionForQuality(quality),
                    quality = quality,
                    basePrice = price,
                    quantity = 1,
                    icon = icon
                )
            }
        }
        return drops
    }

    private fun rollQuality(difficulty: Int, luck: Int): ItemQuality {
        val baseChance = (difficulty + luck / 5).coerceIn(1, 100)
        val roll = Random.nextInt(0, 100)
        return when {
            roll > 98 -> ItemQuality.LEGENDARY
            roll > 90 -> ItemQuality.EPIC
            roll > 70 -> ItemQuality.RARE
            roll > 40 -> ItemQuality.UNCOMMON
            else -> ItemQuality.COMMON
        }
    }

    private fun dropChanceForQuality(quality: ItemQuality, difficulty: Int, luck: Int): Float {
        val modifier = (difficulty * 2 + luck / 3).coerceIn(5, 90) / 100f
        return when (quality) {
            ItemQuality.COMMON -> 0.8f
            ItemQuality.UNCOMMON -> 0.5f * modifier
            ItemQuality.RARE -> 0.25f * modifier
            ItemQuality.EPIC -> 0.12f * modifier
            ItemQuality.LEGENDARY -> 0.05f * modifier
        }.coerceIn(0.05f, 0.95f)
    }

    private fun pickNameForQuality(quality: ItemQuality): String {
        val pool = when (quality) {
            ItemQuality.COMMON -> weaponNames
            ItemQuality.UNCOMMON -> armorNames
            ItemQuality.RARE -> trinketNames
            ItemQuality.EPIC -> weaponNames + armorNames
            ItemQuality.LEGENDARY -> trinketNames + armorNames
        }
        return pool.random()
    }

    private fun descriptionForQuality(quality: ItemQuality): String {
        return when (quality) {
            ItemQuality.COMMON -> "Обычный предмет, пригодный для продажи или простого улучшения."
            ItemQuality.UNCOMMON -> "Редкий предмет с лёгким магическим свечением."
            ItemQuality.RARE -> "Настоящая находка, повышает ваши шансы в приключении."
            ItemQuality.EPIC -> "Эпический артефакт с собственной историей."
            ItemQuality.LEGENDARY -> "Легендарная реликвия, о которой слагают песни."
        }
    }
}

