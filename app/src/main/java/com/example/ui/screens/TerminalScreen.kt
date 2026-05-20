package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalCyan
import com.example.ui.viewmodel.CodexViewModel
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(viewModel: CodexViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val recentCommands by viewModel.recentCommands.collectAsState()
    val curDirectory by viewModel.currentDirectoryPath.collectAsState()

    var inputCmd by remember { mutableStateOf("") }

    val shortcutCmds = listOf(
        "ls", "proot-distro status", "cd ..", "node -v", "npm -v", "cat README.md", "mkdir scripts"
    )

    // Scroll to bottom when new command log is added
    LaunchedEffect(recentCommands.size) {
        if (recentCommands.isNotEmpty()) {
            listState.animateScrollToItem(recentCommands.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Header Stats & Actions ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Console",
                    tint = TerminalCyan,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "bash - ubuntu@localhost:${curDirectory}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = TerminalCyan
                )
            }

            IconButton(
                onClick = { viewModel.clearTerminalLogs() }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear console",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // --- Console History Stream ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recentCommands.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Ubuntu PRoot Terminal Инициализирован",
                            color = TerminalCyan.copy(0.7f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Введите команду или нажмите быстрые теги ниже...",
                            color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                items(recentCommands.reversed()) { cmd ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // User command prompt
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ubuntu@localhost:${cmd.directory}$",
                                color = TerminalCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = cmd.command,
                                color = TerminalGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        // Terminal output
                        Text(
                            text = cmd.output,
                            color = if (cmd.isSuccess) Color(0xFFD4D4D4) else MaterialTheme.colorScheme.error,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // --- Quick Commands row ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(shortcutCmds) { tag ->
                SuggestionChip(
                    onClick = {
                        inputCmd = tag
                    },
                    label = {
                        Text(
                            text = tag,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }

        // --- Console Input Box ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "prompt_prefix",
                    tint = TerminalCyan,
                    modifier = Modifier.size(24.dp)
                )

                OutlinedTextField(
                    value = inputCmd,
                    onValueChange = { inputCmd = it },
                    placeholder = {
                        Text(
                            "Введите bash команду...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputCmd.isNotBlank()) {
                                viewModel.runTerminalCommand(inputCmd)
                                inputCmd = ""
                            }
                        }
                    )
                )

                IconButton(
                    onClick = {
                        if (inputCmd.isNotBlank()) {
                            viewModel.runTerminalCommand(inputCmd)
                            inputCmd = ""
                        }
                    },
                    enabled = inputCmd.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Submit command",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
