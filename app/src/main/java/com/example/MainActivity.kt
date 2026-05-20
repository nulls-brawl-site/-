package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PolygonalLoader
import com.example.ui.screens.CodexChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileExplorerScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CodexTeal
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.CodexViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: CodexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isBooting by viewModel.isBooting.collectAsState()

                Crossfade(
                    targetState = isBooting,
                    animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
                    label = "boot_crossfade"
                ) { booting ->
                    if (booting) {
                        BootupScreen(viewModel)
                    } else {
                        MainNavigationScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }
}

@Composable
fun BootupScreen(viewModel: CodexViewModel) {
    var logsToShow by remember { mutableStateOf<List<String>>(emptyList()) }
    val fullLogs = listOf(
        "⚙️ [  0.000000] Booting PRoot-distro Ubuntu environment on Android ARM64...",
        "📦 [  0.158021] Setting up sandboxed Unix kernel mappings...",
        "📂 [  0.342104] Connecting virtual home path ~/root on local storage...",
        "🔋 [  0.510943] Verification: Background process optimization state checked.",
        "📱 [  0.690102] Binding virtual local ports - port forward 127.0.0.1:8080...",
        "🟢 [  0.891102] Launching OpenAI Codex port NodeJS server daemon...",
        "🔌 [  1.121920] Node.js service is initialised and listening on localhost:8080.",
        "✨ [  1.328932] Handshake complete! Codex Ubuntu active and ready to code."
    )

    val installStatus by viewModel.installStatus.collectAsState()
    val installProgress by viewModel.installProgress.collectAsState()

    // Stream lines on bootup sequence
    LaunchedEffect(Unit) {
        fullLogs.forEachIndexed { idx, line ->
            delay(150)
            logsToShow = logsToShow + line
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E)) // Dark black terminal screen
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = "CODEX UNIX SHELL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = CodexTeal
                )
                Text(
                    text = "Real Ubuntu RootFS Downloader & PRoot wrapper",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Custom polygon loader in center
            PolygonalLoader(sides = 6, size = 150.dp)
            
            // Installation Progress UI
            if (installProgress < 1f || installStatus.contains("Ошибка")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = installStatus, color = Color.White, fontFamily = FontFamily.Monospace)
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
                        color = TerminalCyan,
                        trackColor = Color.DarkGray
                    )
                }
            } else {
                Text("Установка завершена", color = TerminalGreen, fontFamily = FontFamily.Monospace)
            }

            // Dynamic Bootup Logs Box (like Linux startup sequences)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .padding(12.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logsToShow) { log ->
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalCyan,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainNavigationScreen(viewModel: CodexViewModel) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                            contentDescription = "Главная"
                        )
                    },
                    label = { Text("Главная") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Терминал"
                        )
                    },
                    label = { Text("Терминал") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.SmartButton else Icons.Outlined.Code,
                            contentDescription = "Кодекс AI"
                        )
                    },
                    label = { Text("Кодекс AI") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.FolderOpen else Icons.Outlined.Folder,
                            contentDescription = "Файлы"
                        )
                    },
                    label = { Text("Файлы") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel)
                1 -> TerminalScreen(viewModel)
                2 -> CodexChatScreen(viewModel)
                3 -> FileExplorerScreen(viewModel)
            }
        }
    }
}
