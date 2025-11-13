package com.irlquest.app.feature.hero

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.UserDto
import com.irlquest.app.feature.auth.AuthViewModel
import com.irlquest.app.data.repository.InventoryRepository
import com.irlquest.app.data.repository.OwnedItem
import com.irlquest.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroProfileScreen(
    authViewModel: AuthViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val characterRepo = remember { com.irlquest.app.data.repository.CharacterRepository() }
    val inventoryRepository = remember { InventoryRepository(authViewModel) }
    val inventoryItems by inventoryRepository.items.collectAsState()
    var characterProfile by remember { mutableStateOf<com.irlquest.app.data.network.dto.CharacterProfile?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.fetchMe()
        try {
            characterProfile = characterRepo.getCharacterProfile()
        } catch (e: Exception) {
            // Игнорируем ошибку, используем данные из currentUser
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Профиль Героя",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TavernWood,
                    titleContentColor = PrimaryLight
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Аватар и базовая информация
                HeroAvatarSection(currentUser)
            }

            item {
                // Уровень и опыт
                LevelAndExperienceSection(currentUser)
            }

            item {
                // D&D Характеристики (используем данные из Character API если доступны)
                CharacteristicsSection(
                    user = currentUser,
                    characterProfile = characterProfile
                )
            }

            item {
                // Класс и раса
                ClassAndRaceSection(currentUser)
            }

            item {
                // Статистика достижений
                AchievementsSection(currentUser)
            }

            item {
                HeroEquipmentSection(inventoryItems)
            }
        }
    }
}

@Composable
private fun HeroAvatarSection(user: UserDto?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватар
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCharacterIcon(user?.characterClass ?: "warrior"),
                    fontSize = 64.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.username ?: "Гость",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "${getClassName(user?.characterClass)} ${getRaceName(user?.characterRace)}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (user?.bio?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.bio,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun LevelAndExperienceSection(user: UserDto?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Уровень ${user?.level ?: 1}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ранг: ${getRankName(user?.level ?: 1)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user?.gold ?: 0}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Полоса опыта с анимацией
            val targetProgress = calculateXPProgress(user?.experience ?: 0, user?.level ?: 1)
            val animatedProgress by animateFloatAsState(
                targetValue = targetProgress,
                label = "xp_progress"
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Опыт",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${user?.experience ?: 0} / ${getXPForLevel(user?.level ?: 1)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = animatedProgress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CharacteristicsSection(
    user: UserDto?,
    characterProfile: com.irlquest.app.data.network.dto.CharacterProfile?
) {
    // Используем данные из Character API если доступны, иначе из UserDto
    val stats = characterProfile?.character?.stats
    val strength = stats?.strength ?: user?.strength ?: 10
    val intelligence = stats?.intelligence ?: user?.intelligence ?: 10
    val charisma = stats?.charisma ?: user?.charisma ?: 10
    val dexterity = stats?.dexterity ?: user?.dexterity ?: 10
    // CharacterStats не содержит constitution и wisdom, используем из UserDto
    val constitution = user?.constitution ?: 10
    val wisdom = user?.wisdom ?: 10
    val luck = stats?.luck ?: 10

    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Характеристики",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Характеристики в два столбца
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CharacteristicCard(
                        "STR",
                        "Сила",
                        strength,
                        modifier = Modifier.weight(1f)
                    )
                    CharacteristicCard(
                        "INT",
                        "Интеллект",
                        intelligence,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CharacteristicCard(
                        "CHA",
                        "Харизма",
                        charisma,
                        modifier = Modifier.weight(1f)
                    )
                    CharacteristicCard(
                        "DEX",
                        "Ловкость",
                        dexterity,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CharacteristicCard(
                        "CON",
                        "Телосложение",
                        constitution,
                        modifier = Modifier.weight(1f)
                    )
                    CharacteristicCard(
                        "LUCK",
                        "Удача",
                        luck,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Карточка характеристики героя
 * @param icon Иконка/аббревиатура характеристики (STR, INT, DEX и т.д.)
 * @param name Название характеристики
 * @param value Значение характеристики
 * @param modifier Модификатор для настройки размера и расположения
 */
@Composable
private fun CharacteristicCard(
    icon: String,
    name: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Text(
                text = name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = getModifier(value),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ClassAndRaceSection(user: UserDto?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "👤 Происхождение",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoCard(
                    title = "Класс",
                    value = getClassName(user?.characterClass),
                    icon = "Class",
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "Раса",
                    value = getRaceName(user?.characterRace),
                    icon = "Race",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun AchievementsSection(user: UserDto?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Достижения",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Достижения появятся здесь по мере вашего прогресса в приключениях!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Примеры достижений (заглушки)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementBadge("1", "Первый Шаг", true)
                AchievementBadge("2", "Новичок", user?.level ?: 1 >= 2)
                AchievementBadge("3", "Квестодатель", false)
                AchievementBadge("4", "Мастер Дел", false)
            }
        }
    }
}

@Composable
private fun HeroEquipmentSection(items: List<OwnedItem>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Инвентарь",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Text(
                    text = "Предметы появятся после завершения миссий. Попробуйте кооперативные задания, чтобы получить редкую добычу!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.take(5).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 1.dp,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${item.icon} ${item.name}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    item.description?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Text(
                                    text = "×${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    if (items.size > 5) {
                        Text(
                            text = "… и ещё ${items.size - 5} предметов. Загляните в аукцион, чтобы продать лишнее!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Бейдж достижения
 * @param icon Иконка/символ достижения
 * @param title Название достижения
 * @param unlocked Разблокировано ли достижение
 */
@Composable
private fun AchievementBadge(
    icon: String,
    title: String,
    unlocked: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (unlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
                .border(
                    2.dp,
                    if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 28.sp,
                modifier = Modifier.alpha(if (unlocked) 1f else 0.3f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

// Вспомогательные функции
private fun getCharacterIcon(classKey: String): String {
    return when (classKey.lowercase()) {
        "warrior" -> "W"
        "mage" -> "M"
        "rogue" -> "R"
        "cleric" -> "C"
        "ranger" -> "Rg"
        else -> "?"
    }
}

private fun getClassName(classKey: String?): String {
    return when (classKey?.lowercase()) {
        "warrior" -> "Воин"
        "mage" -> "Маг"
        "rogue" -> "Плут"
        "cleric" -> "Жрец"
        "ranger" -> "Следопыт"
        else -> "Искатель приключений"
    }
}

private fun getRaceName(raceKey: String?): String {
    return when (raceKey?.lowercase()) {
        "human" -> "Человек"
        "elf" -> "Эльф"
        "dwarf" -> "Дворф"
        "halfling" -> "Полурослик"
        "orc" -> "Орк"
        else -> "Человек"
    }
}

private fun calculateXPProgress(xp: Int, level: Int): Float {
    val requiredXP = getXPForLevel(level)
    return (xp.toFloat() / requiredXP).coerceIn(0f, 1f)
}

private fun getXPForLevel(level: Int): Int {
    return level * 100
}

private fun getRankName(level: Int): String {
    return when {
        level < 5 -> "Новичок"
        level < 10 -> "Искатель"
        level < 20 -> "Герой"
        level < 30 -> "Чемпион"
        level < 50 -> "Легенда"
        else -> "Мифический герой"
    }
}

private fun getModifier(value: Int): String {
    val modifier = (value - 10) / 2
    return if (modifier >= 0) "+$modifier" else "$modifier"
}

