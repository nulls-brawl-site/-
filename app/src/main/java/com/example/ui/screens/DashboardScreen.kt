package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PolygonalLoader
import com.example.ui.viewmodel.CodexViewModel

@Composable
fun DashboardScreen(viewModel: CodexViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val isDaemonActive by viewModel.isDaemonActive.collectAsState()
    val ramUsage by viewModel.ramUsageMb.collectAsState()
    val cpuUsage by viewModel.cpuUsagePercent.collectAsState()
    val isFilesGranted by viewModel.isFilePermissionGranted.collectAsState()
    val isBatteryIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()

    // Gauge animations
    val cpuProgress by animateFloatAsState(
        targetValue = cpuUsage / 100f,
        animationSpec = tween(1000),
        label = "cpu_gauge"
    )
    val ramProgress by animateFloatAsState(
        targetValue = ramUsage / 1024f,
        animationSpec = tween(1000),
        label = "ram_gauge"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Ubuntu status",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Column {
                    Text(
                        text = "Ubuntu 24.04 (PRoot-Distro)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDaemonActive) Color(0xFF4AF626) else Color.Red)
                        )
                        Text(
                            text = if (isDaemonActive) "Активен • Node.js Codex Port запущен" else "Инициализация...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // --- System Resources Circular Gauge widgets ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CPU Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Нагрузка CPU", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { cpuProgress },
                            modifier = Modifier.size(72.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                        Text("${cpuUsage}%", fontWeight = FontWeight.Bold)
                    }
                    Text("Виртуальное ядро", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }

            // RAM Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ОЗУ Node.js", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { ramProgress },
                            modifier = Modifier.size(72.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            strokeWidth = 6.dp
                        )
                        Text("${ramUsage}M", fontWeight = FontWeight.Bold)
                    }
                    Text("Лимит: 1024M", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }
        }

        // --- System Permission Triggers Card ---
        Text(
            text = "Настройки Системы и Разрешения",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // 1. Storage Permission
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderShared,
                        contentDescription = "Files Access",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text("Доступ ко всем файлам", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Позволит Codex создавать файлы в /sdcard/",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
                Switch(
                    checked = isFilesGranted,
                    onCheckedChange = { viewModel.requestFilesPermission(context) }
                )
            }
        }

        // 2. Battery Exemption Permission
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BatteryAlert,
                        contentDescription = "Battery info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text("Контроль активности/Батарея", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Работа в фоне без ограничений",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                }
                Switch(
                    checked = isBatteryIgnored,
                    onCheckedChange = { viewModel.requestBatteryOptimizationExemption(context) }
                )
            }
        }

        // 3. Phantom Process Killer Remover Guide & Triggers
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Phantom processes",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Лимит Phantom Processes (Android 12+)", fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Android может автоматически останавливать Ubuntu в фоне при большой активности. " +
                            "Рекомендуется выполнить adb команду (через LADB или компьютер), чтобы отключить лимит:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.getPhantomKillerDisableCommand(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(viewModel.getPhantomKillerDisableCommand()))
                                Toast.makeText(context, "Команда скопирована!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy adb command")
                        }
                    }
                }
            }
        }
    }
}
