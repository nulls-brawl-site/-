package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Code
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.PolygonalLoader
import com.example.ui.theme.CodexTeal
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.CodexViewModel

@Composable
fun CodexChatScreen(viewModel: CodexViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val messages by viewModel.codexMessages.collectAsState()
    val isAwaitingResponse by viewModel.isAwaitingResponse.collectAsState()

    var chatInput by remember { mutableStateOf("") }

    // Auto scroll to bottom
    LaunchedEffect(messages.size, isAwaitingResponse) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Chat Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartButton,
                    contentDescription = "AI Port active",
                    tint = CodexTeal,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "OpenAI Codex Agent",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Встроенный Node.js Кодекс порт",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            }

            IconButton(
                onClick = { viewModel.clearCodexChat() }
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Clear Session",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }

        // --- Chat Logs View Stream ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PolygonalLoader(sides = 6, size = 96.dp)

                        Text(
                            text = "Кодекс Порт готов к созданию проектов",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = CodexTeal,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Напишите, что вы хотите создать, или выполните команду!\n\n" +
                                    "Пример: 'Создай python скрипт в /sdcard/parser.py который скачивает html сайта'\n" +
                                    "и Codex автоматически запишет файл и выполнит команду за вас!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(messages) { msg ->
                    val isUser = msg.isUser
                    val isSystemAuto = msg.isCommandResult

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = when {
                                isUser -> MaterialTheme.colorScheme.primaryContainer
                                isSystemAuto -> Color(0xFF1E2822) // Emerald shade for auto action logs
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 4.dp,
                                bottomEnd = if (isUser) 4.dp else 16.dp
                            ),
                            border = if (isSystemAuto) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4AF626).copy(0.3f)) else null,
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isSystemAuto) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoMode,
                                            contentDescription = "Auto Executed",
                                            tint = TerminalGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Кодекс Авто-Раннер",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TerminalGreen
                                        )
                                    }
                                }

                                if (msg.content.contains("```") && !isUser) {
                                    // Parse code block nicely
                                    val parts = msg.content.split("```")
                                    parts.forEachIndexed { idx, p ->
                                        if (idx % 2 == 1) {
                                            // Code block
                                            val cleanCode = p.substringAfter("\n").trim()
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable {
                                                        clipboardManager.setText(AnnotatedString(cleanCode))
                                                        Toast.makeText(context, "Код скопирован!", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Код (нажми, чтобы копировать)", fontSize = 10.sp, color = CodexTeal)
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = CodexTeal, modifier = Modifier.size(12.dp))
                                                    }
                                                    Text(
                                                        text = cleanCode,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFFD4D4D4)
                                                    )
                                                }
                                            }
                                        } else if (p.isNotEmpty()) {
                                            Text(
                                                text = p.trim(),
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = msg.content,
                                        fontSize = 13.sp,
                                        color = if (isSystemAuto) TerminalGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isAwaitingResponse) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PolygonalLoader(sides = 5, size = 32.dp, strokeWidth = 3f)
                        Text(
                            text = "Кодекс компилирует и выполняет инструкции...",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CodexTeal
                        )
                    }
                }
            }
        }

        // --- Chat Keyboard input ---
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
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "prompt_ai",
                    tint = CodexTeal,
                    modifier = Modifier.size(24.dp)
                )

                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = {
                        Text(
                            "Напишите промпт для Кодекса...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Default
                    )
                )

                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            viewModel.sendCodexPrompt(chatInput)
                            chatInput = ""
                        }
                    },
                    enabled = chatInput.isNotBlank() && !isAwaitingResponse,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CodexTeal,
                        contentColor = Color.Black,
                        disabledContainerColor = CodexTeal.copy(0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send prompt",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
