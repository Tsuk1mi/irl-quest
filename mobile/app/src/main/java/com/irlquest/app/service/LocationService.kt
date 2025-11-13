package com.irlquest.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.irlquest.app.data.repository.GeolocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Состояние геолокации пользователя
 * @param latitude Широта текущей позиции
 * @param longitude Долгота текущей позиции
 * @param accuracy Точность определения позиции в метрах
 * @param isEnabled Включена ли геолокация
 * @param error Сообщение об ошибке, если есть
 */
data class LocationState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val isEnabled: Boolean = false,
    val error: String? = null
)

/**
 * Сервис для работы с геолокацией
 * Управляет получением координат пользователя и проверкой геозон для квестов
 */
class LocationService(
    private val context: Context,
    private val geoRepo: GeolocationRepository = GeolocationRepository()
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    /**
     * Слушатель изменений геолокации
     * Обновляет состояние при получении новых координат
     */
    private val locationListener = LocationListener { location ->
        _locationState.value = LocationState(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            isEnabled = true
        )
    }

    /**
     * Проверяет наличие разрешений на геолокацию
     * @return true если разрешения предоставлены
     */
    fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Проверяет, включена ли геолокация в настройках устройства
     * @return true если GPS или Network провайдер включен
     */
    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Начинает отслеживание геолокации
     * Использует GPS провайдер, если доступен, иначе Network провайдер
     */
    fun startLocationUpdates() {
        if (!checkPermissions()) {
            _locationState.value = LocationState(
                error = "Разрешения на геолокацию не предоставлены"
            )
            return
        }

        if (!isLocationEnabled()) {
            _locationState.value = LocationState(
                error = "Геолокация отключена в настройках"
            )
            return
        }

        try {
            // Выбираем провайдер: предпочитаем GPS для точности
            val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else {
                LocationManager.NETWORK_PROVIDER
            }

            // Запрашиваем обновления каждые 10 секунд или при перемещении на 10 метров
            locationManager.requestLocationUpdates(
                provider,
                10000L, // Интервал обновления: 10 секунд
                10f, // Минимальное расстояние: 10 метров
                locationListener
            )

            // Получаем последнюю известную локацию для немедленного отображения
            val lastLocation = locationManager.getLastKnownLocation(provider)
            lastLocation?.let {
                _locationState.value = LocationState(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    isEnabled = true
                )
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to start location updates")
            _locationState.value = LocationState(
                error = "Ошибка доступа к геолокации: ${e.message}"
            )
        }
    }

    /**
     * Останавливает отслеживание геолокации
     * Освобождает ресурсы и останавливает слушатель
     */
    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop location updates")
        }
    }

    /**
     * Проверяет, находится ли пользователь в геозоне квеста
     * @param questId ID квеста для проверки (опционально)
     * @return Result с результатом проверки или ошибкой
     */
    suspend fun checkLocationForQuest(questId: Int?): Result<com.irlquest.app.data.network.dto.LocationCheckResponse> {
        val state = _locationState.value
        return if (state.latitude != null && state.longitude != null) {
            try {
                val response = geoRepo.checkLocation(
                    state.latitude!!,
                    state.longitude!!,
                    questId
                )
                Result.success(response)
            } catch (e: Exception) {
                Timber.e(e, "Failed to check location")
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("Локация недоступна"))
        }
    }
}
