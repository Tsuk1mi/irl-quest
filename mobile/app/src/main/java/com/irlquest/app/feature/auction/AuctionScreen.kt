package com.irlquest.app.feature.auction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.irlquest.app.TokenStorage
import com.irlquest.app.data.repository.InventoryRepository
import com.irlquest.app.data.repository.OwnedItem
import com.irlquest.app.ui.theme.OnSurface
import com.irlquest.app.ui.theme.Primary
import com.irlquest.app.ui.theme.Surface
import com.irlquest.app.ui.theme.TavernWood
import com.irlquest.shared.models.AuctionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class AuctionUiState(
    val listings: List<AuctionItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val purchaseMessage: String? = null
)

class AuctionViewModel : ViewModel() {
    private val auctionRepository = com.irlquest.app.data.SharedRepositoryProvider.auctionRepository
    private val inventoryRepository = InventoryRepository(null, viewModelScope)

    private val _uiState = MutableStateFlow(AuctionUiState())
    val uiState: StateFlow<AuctionUiState> = _uiState

    val inventory: StateFlow<List<OwnedItem>> = inventoryRepository.items

    init {
        refreshListings()
    }

    fun refreshListings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = TokenStorage.getToken()
                val listings = auctionRepository.getListings(token)
                _uiState.value = _uiState.value.copy(
                    listings = listings,
                    isLoading = false
                )
            } catch (e: Exception) {
                Timber.e(e, "AuctionViewModel: failed to fetch listings")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Не удалось загрузить аукцион"
                )
            }
        }
    }

    fun listItem(item: OwnedItem, price: Int) {
        viewModelScope.launch {
            try {
                val token = TokenStorage.getToken()
                if (token.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(error = "Необходима авторизация для размещения лота")
                    return@launch
                }
                val result = inventoryRepository.listItemOnAuction(
                    localId = item.localId,
                    price = price
                )
                if (result != null) {
                    _uiState.value = _uiState.value.copy(
                        purchaseMessage = "Лот ${item.name} выставлен за ${price} золота!"
                    )
                    refreshListings()
                }
            } catch (e: Exception) {
                Timber.e(e, "AuctionViewModel: failed to list item")
                _uiState.value = _uiState.value.copy(error = "Не удалось выставить предмет: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(purchaseMessage = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(
    onBack: () -> Unit,
    viewModel: AuctionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    var itemToList by remember { mutableStateOf<OwnedItem?>(null) }
    var price by remember { mutableIntStateOf(100) }

    LaunchedEffect(uiState.error) {
        // Could show snackbar
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аукцион Героев", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshListings() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TavernWood
                )
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📦 Мои предметы",
                style = MaterialTheme.typography.titleMedium,
                color = TavernWood
            )
            if (inventory.isEmpty()) {
                Text(
                    text = "Завершайте квесты, чтобы находить экипировку и продавать её на аукционе.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    items(inventory) { item ->
                        InventoryItemCard(
                            item = item,
                            onList = {
                                itemToList = item
                                price = (item.basePrice * item.quantity).coerceAtLeast(10)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🏛️ Лоты Лиги Приключенцев",
                style = MaterialTheme.typography.titleMedium,
                color = TavernWood
            )

            if (uiState.isLoading) {
                Text("Загрузка аукциона...", color = OnSurface.copy(alpha = 0.7f))
            } else if (uiState.listings.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Primary.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎲 Аукцион героев",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = TavernWood
                        )
                        Text(
                            text = "Функция глобального аукциона находится в разработке на стороне сервера.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            text = "Пока вы можете собирать предметы из квестов. Торговля между игроками скоро будет доступна!",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.listings.take(10).forEach { listing ->
                        AuctionListingCard(listing)
                    }
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            uiState.purchaseMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(message, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    itemToList?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { itemToList = null },
            containerColor = Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Выставить ${item.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TavernWood
                )
                OutlinedTextField(
                    value = price.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { price = it.coerceAtLeast(1) }
                    },
                    label = { Text("Цена (золото)") },
                    leadingIcon = { Text("💰") }
                )
                Button(
                    onClick = {
                        viewModel.listItem(item, price)
                        itemToList = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Выставить лот")
                }
                TextButton(
                    onClick = { itemToList = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: OwnedItem,
    onList: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${item.icon} ${item.name}", fontWeight = FontWeight.Bold)
                Text("Качество: ${item.quality.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                Text("Количество: ${item.quantity}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onList) {
                Text("Продать")
            }
        }
    }
}

@Composable
private fun AuctionListingCard(listing: AuctionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "${listing.name} • ${listing.quality.name.lowercase()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            listing.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface.copy(alpha = 0.7f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Цена: ${listing.price} золота", fontWeight = FontWeight.Bold)
                Text("Продавец: ${listing.seller}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

