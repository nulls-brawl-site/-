package com.example.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.CodexViewModel
import java.io.File

@Composable
fun FileExplorerScreen(viewModel: CodexViewModel) {
    val context = LocalContext.current
    val isFilesGranted by viewModel.isFilePermissionGranted.collectAsState()

    var activeDirType by remember { mutableStateOf("root") } // "root" or "sdcard"
    val baseRoot = remember { File(context.filesDir, "root") }
    val baseSdcard = remember { Environment.getExternalStorageDirectory() }

    var currentDir by remember { mutableStateOf(baseRoot) }
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }

    var selectedFileStream by remember { mutableStateOf<File?>(null) }
    var fileContentToShow by remember { mutableStateOf("") }

    // Sync file list when current directory or tab changes
    val refreshFiles = {
        try {
            if (activeDirType == "sdcard" && !isFilesGranted) {
                fileList = emptyList()
            } else {
                val files = currentDir.listFiles()?.toList() ?: emptyList()
                // Sort folders first, then files alphabetically
                fileList = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            }
        } catch (e: Exception) {
            fileList = emptyList()
        }
    }

    LaunchedEffect(currentDir, activeDirType, isFilesGranted) {
        refreshFiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Navigation Toggle Chips ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeDirType == "root",
                onClick = {
                    activeDirType = "root"
                    currentDir = baseRoot
                },
                leadingIcon = { Icon(Icons.Default.Home, "ubuntu root icon") },
                label = { Text("Ubuntu Root ~") }
            )

            FilterChip(
                selected = activeDirType == "sdcard",
                onClick = {
                    activeDirType = "sdcard"
                    currentDir = baseSdcard
                },
                leadingIcon = { Icon(Icons.Default.SdCard, "sdcard icon") },
                label = { Text("SD Card /sdcard") }
            )
        }

        // --- Current Directory Path Bar ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FolderOpen, "current folder", tint = TerminalCyan)
                Text(
                    text = currentDir.absolutePath
                        .replace(context.filesDir.absolutePath + "/root", "~")
                        .replace(context.filesDir.absolutePath, "app_data"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    color = TerminalCyan,
                    modifier = Modifier.weight(1f)
                )

                if (currentDir != baseRoot && currentDir != baseSdcard) {
                    IconButton(
                        onClick = {
                            val parent = currentDir.parentFile
                            if (parent != null) {
                                currentDir = parent
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, "go parent")
                    }
                }
            }
        }

        // --- File System Empty State or Warning ---
        if (activeDirType == "sdcard" && !isFilesGranted) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permission needed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Доступ к SD-карте заблокирован",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Разрешите 'Доступ ко всем файлам' во вкладке Главная, чтобы Codex мог управлять данными на встроенной SD-карте.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Button(
                    onClick = { viewModel.requestFilesPermission(context) }
                ) {
                    Text("Предоставить Разрешение")
                }
            }
        } else if (fileList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Папка пуста. Здесь будут отображаться созданные файлы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                )
            }
        } else {
            // --- File/Folder List ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fileList) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (file.isDirectory) {
                                        currentDir = file
                                    } else {
                                        selectedFileStream = file
                                        fileContentToShow = try {
                                            file.readText()
                                        } catch (e: Exception) {
                                            "Ошибка чтения: ${e.message}"
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                                    contentDescription = if (file.isDirectory) "dir" else "file",
                                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else TerminalCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!file.isDirectory) {
                                        Text(
                                            text = "${file.length()} b | ${file.name.substringAfterLast('.', "")}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                        )
                                    }
                                }
                            }

                            // Action Menu (Delete Button)
                            Row {
                                IconButton(
                                    onClick = {
                                        try {
                                            val ok = file.deleteRecursively()
                                            if (ok) {
                                                Toast.makeText(context, "Удалено!", Toast.LENGTH_SHORT).show()
                                                refreshFiles()
                                            } else {
                                                Toast.makeText(context, "Не удалось удалить", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- File Content Reader Dialog ---
        selectedFileStream?.let { file ->
            AlertDialog(
                onDismissRequest = { selectedFileStream = null },
                confirmButton = {
                    TextButton(onClick = { selectedFileStream = null }) {
                        Text("Закрыть")
                    }
                },
                title = { Text(file.name, style = MaterialTheme.typography.titleMedium) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = fileContentToShow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    }
}
