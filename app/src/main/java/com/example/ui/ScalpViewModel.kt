package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScalpViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ScalpRepository(database.scalpDao())

    // === App SharedPreferences Configuration ===
    private val sharedPrefs = application.getSharedPreferences("scalp_care_prefs", Context.MODE_PRIVATE)

    // Current operating date (format: yyyy-MM-dd)
    private val _selectedDate = MutableStateFlow(getTodayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // PIN lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isPinSetupRequired = MutableStateFlow(false)
    val isPinSetupRequired: StateFlow<Boolean> = _isPinSetupRequired.asStateFlow()

    // Check if user has notification enabled (Saved offline inside SharedPreferences)
    private val _notificationsEnabled = MutableStateFlow(sharedPrefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Base preferred angle configuration ("정면", "위쪽", "양쪽 측면", "정수리")
    private val _defaultAngleType = MutableStateFlow(sharedPrefs.getString("default_angle_type", "정면") ?: "정면")
    val defaultAngleType: StateFlow<String> = _defaultAngleType.asStateFlow()

    // === Database Flow Streams ===
    val allPhotoRecords: StateFlow<List<HairPhotoRecord>> = repository.allPhotoRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScalpRecords: StateFlow<List<ScalpConditionRecord>> = repository.allScalpRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutines: StateFlow<List<HairCareRoutine>> = repository.allRoutinesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRoutines: StateFlow<List<HairCareRoutine>> = repository.activeRoutinesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected date's scalp record
    val currentScalpRecord: StateFlow<ScalpConditionRecord?> = _selectedDate
        .flatMapLatest { date -> repository.getScalpRecordFlowByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selected date's routine checks
    val currentChecks: StateFlow<List<HairCareCheck>> = _selectedDate
        .flatMapLatest { date -> repository.getChecksByDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // App Lock check on launch
        checkPinStatus()
    }

    // === Core Actions ===

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Save/Update Scalp Condition
    fun saveScalpCondition(
        scalpState: List<String>,
        hairState: List<String>,
        stressScore: Int,
        sleepHours: Float,
        alcohol: Boolean,
        exercise: Boolean,
        memo: String
    ) {
        viewModelScope.launch {
            val existing = repository.getScalpRecordByDate(_selectedDate.value)
            val record = ScalpConditionRecord(
                id = existing?.id ?: 0,
                date = _selectedDate.value,
                scalpState = scalpState.joinToString(","),
                hairState = hairState.joinToString(","),
                stressScore = stressScore,
                sleepHours = sleepHours,
                alcohol = alcohol,
                exercise = exercise,
                memo = memo
            )
            repository.insertScalpRecord(record)
        }
    }

    // Toggle Checklist state
    fun toggleRoutineCheck(routineId: Int, isChecked: Boolean) {
        viewModelScope.launch {
            repository.saveCheck(routineId, _selectedDate.value, isChecked)
        }
    }

    // Add Custom Routine
    fun addNewRoutine(title: String, category: String = "OTHER") {
        viewModelScope.launch {
            val routine = HairCareRoutine(
                title = title,
                category = category,
                isActive = true
            )
            repository.insertRoutine(routine)
        }
    }

    // Toggle Routine Active Status
    fun toggleRoutineActive(routine: HairCareRoutine, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateRoutine(routine.copy(isActive = isActive))
        }
    }

    // Delete custom routine entirely
    fun deleteRoutine(routine: HairCareRoutine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }

    // Save/Add Photo Record from Camera/Gallery URI
    fun savePhotoRecord(angleType: String, imageUri: Uri, memo: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            val savedLocalPath = copyUriToInternalStorage(imageUri, angleType)
            if (savedLocalPath != null) {
                val record = HairPhotoRecord(
                    date = _selectedDate.value,
                    angleType = angleType,
                    imagePath = savedLocalPath,
                    memo = memo
                )
                repository.insertPhotoRecord(record)
                onFinished()
            }
        }
    }

    // Delete photo record
    fun deletePhotoRecord(record: HairPhotoRecord) {
        viewModelScope.launch {
            // Delete the local file
            try {
                val file = File(record.imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deletePhotoRecord(record)
        }
    }

    // Helper functions to save images
    private fun copyUriToInternalStorage(uri: Uri, angle: String): String? {
        val context = getApplication<Application>()
        return try {
            val directory = File(context.filesDir, "hair_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            val file = File(directory, "photo_${timestamp}_${angle.hashCode()}.jpg")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveCapturedImageFile(tempFile: File, angle: String): String? {
        val context = getApplication<Application>()
        return try {
            val directory = File(context.filesDir, "hair_photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val timestamp = System.currentTimeMillis()
            val destFile = File(directory, "photo_${timestamp}_${angle.hashCode()}.jpg")
            tempFile.renameTo(destFile)
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Save capture direct record
    fun addDirectPhotoRecord(angleType: String, filePath: String, memo: String) {
        viewModelScope.launch {
            val record = HairPhotoRecord(
                date = _selectedDate.value,
                angleType = angleType,
                imagePath = filePath,
                memo = memo
            )
            repository.insertPhotoRecord(record)
        }
    }

    // === PIN / Security Functions ===

    private fun checkPinStatus() {
        val savedPin = sharedPrefs.getString("app_pin", "") ?: ""
        if (savedPin.isNotEmpty()) {
            _isAppLocked.value = true
            _isPinSetupRequired.value = false
        } else {
            _isAppLocked.value = false
            _isPinSetupRequired.value = true
        }
    }

    fun setupPin(newPin: String) {
        sharedPrefs.edit().putString("app_pin", newPin).apply()
        _isAppLocked.value = false
        _isPinSetupRequired.value = false
    }

    fun removePin() {
        sharedPrefs.edit().putString("app_pin", "").apply()
        _isAppLocked.value = false
        _isPinSetupRequired.value = true
    }

    fun verifyPin(enteredPin: String): Boolean {
        val savedPin = sharedPrefs.getString("app_pin", "") ?: ""
        val matches = savedPin == enteredPin
        if (matches) {
            _isAppLocked.value = false
        }
        return matches
    }

    fun toggleAppLockDirectly(lock: Boolean) {
        if (!lock) {
            _isAppLocked.value = false
        } else {
            val savedPin = sharedPrefs.getString("app_pin", "") ?: ""
            if (savedPin.isNotEmpty()) {
                _isAppLocked.value = true
            }
        }
    }

    // === Settings Configuration Toggles ===

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPrefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun setDefaultAngle(angle: String) {
        _defaultAngleType.value = angle
        sharedPrefs.edit().putString("default_angle_type", angle).apply()
    }

    // Wipe all app records
    fun clearDatabaseData() {
        viewModelScope.launch {
            // Delete files inside directory
            try {
                val context = getApplication<Application>()
                val directory = File(context.filesDir, "hair_photos")
                if (directory.exists()) {
                    directory.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.clearAllData()
        }
    }
}
