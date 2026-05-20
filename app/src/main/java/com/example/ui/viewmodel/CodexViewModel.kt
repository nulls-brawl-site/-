package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CodexMessage
import com.example.data.CodexRepository
import com.example.data.TerminalCommand
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class CodexViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = CodexRepository(
        application,
        database.terminalDao(),
        database.codexDao()
    )

    // UI state flows from Room & Flows
    val recentCommands: StateFlow<List<TerminalCommand>> = repository.recentCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codexMessages: StateFlow<List<CodexMessage>> = repository.codexMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDaemonActive: StateFlow<Boolean> = repository.isDaemonActive
    val ramUsageMb: StateFlow<Int> = repository.ramUsageMb
    val cpuUsagePercent: StateFlow<Int> = repository.cpuUsagePercent

    // State tracks
    private val _isBooting = MutableStateFlow(true)
    val isBooting: StateFlow<Boolean> = _isBooting

    private val _isFilePermissionGranted = MutableStateFlow(false)
    val isFilePermissionGranted: StateFlow<Boolean> = _isFilePermissionGranted

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored

    private val _currentDirectoryPath = MutableStateFlow("~")
    val currentDirectoryPath: StateFlow<String> = _currentDirectoryPath

    private val _isAwaitingResponse = MutableStateFlow(false)
    val isAwaitingResponse: StateFlow<Boolean> = _isAwaitingResponse

    init {
        // Run booting animation sequence
        viewModelScope.launch {
            repository.initializeDaemon()
            _isBooting.value = false
        }
        checkPermissions()
    }

    fun checkPermissions() {
        val context = getApplication<Application>()

        // 1. Files access
        _isFilePermissionGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        // 2. Battery optimization status
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _isBatteryOptimizationIgnored.value = pm.isIgnoringBatteryOptimizations(context.packageName)

        // 3. Keep current path updated
        _currentDirectoryPath.value = repository.shellRunner.curDirectory.absolutePath
            .replace(context.filesDir.absolutePath + "/root", "~")
            .replace(context.filesDir.absolutePath, "app_data")
    }

    fun requestFilesPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } else {
            // Older versions use prompt in MainActivity
        }
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Log or fallback
        }
    }

    fun runTerminalCommand(command: String) {
        viewModelScope.launch {
            repository.executeTerminalCommand(command)
            checkPermissions() // Sync directory and file state
        }
    }

    fun sendCodexPrompt(prompt: String) {
        if (prompt.isBlank()) return
        _isAwaitingResponse.value = true
        viewModelScope.launch {
            repository.sendCodexPrompt(prompt)
            _isAwaitingResponse.value = false
            checkPermissions() // Sync file system
        }
    }

    fun clearTerminalLogs() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearCodexChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    // Helper to request Phantom Process killer removal rules instructions
    fun getPhantomKillerDisableCommand(): String {
        return "adb shell device_config put activity_manager max_phantom_processes 2147483647"
    }
}

// Check environmental import helper
object Environment {
    fun isExternalStorageManager(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
    }
}
