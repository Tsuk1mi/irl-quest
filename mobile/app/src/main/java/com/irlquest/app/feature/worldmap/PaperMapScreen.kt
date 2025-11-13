package com.irlquest.app.feature.worldmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.irlquest.app.data.network.dto.QuestDto
import com.irlquest.app.ui.theme.*

/**
 * Экран карты мира с дизайном бумажной карты
 * Отображает квесты в значимых местах (Кремль, музеи, парки и т.д.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperMapScreen(
    onQuestClick: (Int) -> Unit = {},
    viewModel: WorldMapViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationService = remember(context) { 
        context?.let { com.irlquest.app.service.LocationService(it) }
    }
    val locationState by locationService?.locationState?.collectAsState() ?: remember { 
        mutableStateOf(com.irlquest.app.service.LocationState())
    }
    
    var selectedQuest by remember { mutableStateOf<QuestDto?>(null) }
    
    LaunchedEffect(Unit) {
        viewModel.loadZones()
        locationService?.startLocationUpdates()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            locationService?.stopLocationUpdates()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "🗺️ Карта мира",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TavernWood,
                    titleContentColor = PrimaryLight
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF5E6D3), // Пергамент
                            Color(0xFFE8D5B7), // Старая бумага
                            Color(0xFFD4C4A8)  // Пожелтевшая бумага
                        )
                    )
                )
        ) {
            // Бумажная карта с текстурами
            PaperMapCanvas(
                quests = uiState.quests,
                userLocation = locationState.latitude?.let { lat ->
                    locationState.longitude?.let { lon ->
                        Pair(lat, lon)
                    }
                },
                onQuestClick = { quest ->
                    selectedQuest = quest
                    onQuestClick(quest.id)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Легенда карты
            MapLegend(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
            
            // Информация о выбранном квесте
            selectedQuest?.let { quest ->
                QuestInfoCard(
                    quest = quest,
                    onDismiss = { selectedQuest = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Canvas для отрисовки бумажной карты с квестами
 */
@Composable
fun PaperMapCanvas(
    quests: List<QuestDto>,
    userLocation: Pair<Double, Double>?,
    onQuestClick: (QuestDto) -> Unit,
    modifier: Modifier = Modifier
) {
    // Значимые места Москвы с координатами
    val landmarks = remember {
        listOf(
            Landmark("Кремль", 55.7520, 37.6173, "Посетить Кремль"),
            Landmark("Красная Площадь", 55.7539, 37.6208, "Прогуляться по Красной площади"),
            Landmark("Парк Горького", 55.7320, 37.6010, "Провести время в парке"),
            Landmark("ВДНХ", 55.8304, 37.6315, "Изучить выставки"),
            Landmark("Третьяковская Галерея", 55.7415, 37.6208, "Посетить музей"),
            Landmark("Большой Театр", 55.7596, 37.6194, "Сходить на представление"),
            Landmark("Собор Василия Блаженного", 55.7525, 37.6231, "Изучить архитектуру"),
            Landmark("Парк Сокольники", 55.7942, 37.6806, "Активный отдых")
        )
    }
    
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // Проверяем клик по маркерам квестов
                    landmarks.forEachIndexed { index, landmark ->
                        val quest = quests.find { it.title?.contains(landmark.name, ignoreCase = true) == true }
                        if (quest != null) {
                            val markerX = (landmark.longitude + 37.0) * size.width / 2.0f
                            val markerY = (55.8 - landmark.latitude) * size.height / 0.6f
                            val distance = kotlin.math.sqrt(
                                (tapOffset.x - markerX) * (tapOffset.x - markerX) + 
                                (tapOffset.y - markerY) * (tapOffset.y - markerY)
                            )
                            if (distance < 50.dp.toPx()) {
                                onQuestClick(quest)
                            }
                        }
                    }
                }
            }
    ) {
        // Фон карты с текстурой бумаги
        drawPaperTexture()
        
        // Сетка координат
        drawCoordinateGrid()
        
        // Очертания города (упрощенные)
        drawCityOutline()
        
        // Маркеры значимых мест
        landmarks.forEach { landmark ->
            val quest = quests.find { 
                it.title?.contains(landmark.name, ignoreCase = true) == true ||
                it.description?.contains(landmark.name, ignoreCase = true) == true
            }
            
            val x = ((landmark.longitude + 37.0) / 2.0 * size.width).toFloat()
            val y = ((55.8 - landmark.latitude) / 0.6 * size.height).toFloat()
            
            if (quest != null) {
                // Маркер квеста
                drawQuestMarker(
                    center = Offset(x, y),
                    quest = quest,
                    isCompleted = quest.status == "completed"
                )
            } else {
                // Обычный маркер места
                drawLandmarkMarker(
                    center = Offset(x, y),
                    name = landmark.name
                )
            }
        }
        
        // Позиция пользователя
        userLocation?.let { (lat, lon) ->
            val x = ((lon + 37.0) / 2.0 * size.width).toFloat()
            val y = ((55.8 - lat) / 0.6 * size.height).toFloat()
            drawUserLocation(Offset(x, y))
        }
    }
}

/**
 * Текстура бумаги для карты
 */
private fun DrawScope.drawPaperTexture() {
    // Основной фон пергамента
    drawRect(
        color = Color(0xFFF5E6D3),
        size = size
    )
    
    // Текстура состаренной бумаги (точки и линии)
    repeat(100) {
        val x = (0..size.width.toInt()).random().toFloat()
        val y = (0..size.height.toInt()).random().toFloat()
        drawCircle(
            color = Color(0xFFD4C4A8).copy(alpha = 0.1f),
            radius = 2f,
            center = Offset(x, y)
        )
    }
}

/**
 * Сетка координат на карте
 */
private fun DrawScope.drawCoordinateGrid() {
    val strokeColor = Color(0xFF8B7355).copy(alpha = 0.3f)
    val strokeWidth = 1f
    
    // Вертикальные линии
    repeat(5) { i ->
        val x = (size.width / 5) * (i + 1)
        drawLine(
            color = strokeColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
    }
    
    // Горизонтальные линии
    repeat(5) { i ->
        val y = (size.height / 5) * (i + 1)
        drawLine(
            color = strokeColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Очертания города (упрощенные контуры)
 */
private fun DrawScope.drawCityOutline() {
    val path = Path().apply {
        // Упрощенный контур Москвы
        moveTo(size.width * 0.3f, size.height * 0.2f)
        lineTo(size.width * 0.7f, size.height * 0.15f)
        lineTo(size.width * 0.85f, size.height * 0.4f)
        lineTo(size.width * 0.8f, size.height * 0.7f)
        lineTo(size.width * 0.5f, size.height * 0.85f)
        lineTo(size.width * 0.2f, size.height * 0.75f)
        close()
    }
    
    drawPath(
        path = path,
        color = Color(0xFF8B7355).copy(alpha = 0.2f),
        style = Stroke(width = 3f)
    )
}

/**
 * Маркер квеста на карте
 */
private fun DrawScope.drawQuestMarker(
    center: Offset,
    quest: QuestDto,
    isCompleted: Boolean
) {
    val color = if (isCompleted) {
        Color(0xFF4CAF50) // Зеленый для завершенных
    } else {
        when (quest.priority) {
            "critical" -> Color(0xFFD32F2F) // Красный
            "high" -> Color(0xFFFF9800) // Оранжевый
            else -> Color(0xFF2196F3) // Синий
        }
    }
    
    // Внешний круг
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = 30f,
        center = center
    )
    
    // Внутренний круг
    drawCircle(
        color = color,
        radius = 15f,
        center = center
    )
    
    // Иконка меча в центре
    drawLine(
        color = Color.White,
        start = Offset(center.x, center.y - 8f),
        end = Offset(center.x, center.y + 8f),
        strokeWidth = 3f
    )
}

/**
 * Маркер значимого места
 */
private fun DrawScope.drawLandmarkMarker(
    center: Offset,
    name: String
) {
    // Квадратный маркер
    drawRect(
        color = Color(0xFF8B7355),
        topLeft = Offset(center.x - 8f, center.y - 8f),
        size = Size(16f, 16f)
    )
}

/**
 * Позиция пользователя на карте
 */
private fun DrawScope.drawUserLocation(center: Offset) {
    // Внешний пульсирующий круг
    drawCircle(
        color = Color(0xFF2196F3).copy(alpha = 0.3f),
        radius = 25f,
        center = center
    )
    
    // Внутренний круг
    drawCircle(
        color = Color(0xFF2196F3),
        radius = 10f,
        center = center
    )
    
    // Центральная точка
    drawCircle(
        color = Color.White,
        radius = 5f,
        center = center
    )
}

/**
 * Легенда карты
 */
@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Легенда",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            LegendItem(
                color = Color(0xFF2196F3),
                text = "Активные квесты"
            )
            LegendItem(
                color = Color(0xFFD32F2F),
                text = "Критические квесты"
            )
            LegendItem(
                color = Color(0xFFFF9800),
                text = "Высокий приоритет"
            )
            LegendItem(
                color = Color(0xFF4CAF50),
                text = "Завершенные"
            )
            LegendItem(
                color = Color(0xFF2196F3),
                text = "Ваше местоположение"
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Text(
            text = text,
            fontSize = 12.sp
        )
    }
}

/**
 * Карточка информации о квесте
 */
@Composable
fun QuestInfoCard(
    quest: QuestDto,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quest.title ?: "Квест",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Text("X")
                }
            }
            
            quest.description?.let {
                Text(
                    text = it,
                    fontSize = 12.sp
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Приоритет: ${quest.priority ?: "medium"}",
                    fontSize = 12.sp
                )
                Text(
                    text = "Опыт: ${quest.rewardExperience ?: 0}",
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Данные о значимом месте
 */
data class Landmark(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val questDescription: String
)

