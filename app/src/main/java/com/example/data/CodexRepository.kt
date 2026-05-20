package com.example.data

import android.content.Context
import com.example.data.api.Content
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File

class CodexRepository(
    private val context: Context,
    private val terminalDao: TerminalDao,
    private val codexDao: CodexDao
) {
    val shellRunner = ShellRunner(context)
    val recentCommands: Flow<List<TerminalCommand>> = terminalDao.getRecentCommands()
    val codexMessages: Flow<List<CodexMessage>> = codexDao.getAllMessages()

    private val _isDaemonActive = MutableStateFlow(false)
    val isDaemonActive: StateFlow<Boolean> = _isDaemonActive

    private val _ramUsageMb = MutableStateFlow(84)
    val ramUsageMb: StateFlow<Int> = _ramUsageMb

    private val _cpuUsagePercent = MutableStateFlow(2)
    val cpuUsagePercent: StateFlow<Int> = _cpuUsagePercent

    // Starts system daemon simulation instantly on app launch
    suspend fun initializeDaemon() {
        _isDaemonActive.value = false
        // Simulate loading sequence
        kotlinx.coroutines.delay(1200)
        _isDaemonActive.value = true
        _ramUsageMb.value = 84
        _cpuUsagePercent.value = 4
    }

    suspend fun executeTerminalCommand(cmd: String): String {
        val terminalResult = shellRunner.executeCommand(cmd)
        val entity = TerminalCommand(
            command = cmd,
            output = terminalResult.first,
            isSuccess = terminalResult.second,
            directory = shellRunner.curDirectory.name
        )
        terminalDao.insertCommand(entity)
        return terminalResult.first
    }

    suspend fun clearHistory() {
        terminalDao.clearHistory()
    }

    suspend fun clearChat() {
        codexDao.clearChat()
    }

    // Sends prompt to OpenAI Codex Node.js port (represented by Gemini REST API)
    suspend fun sendCodexPrompt(prompt: String): String {
        // Save user message to database
        codexDao.insertMessage(CodexMessage(content = prompt, isUser = true))

        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val errMsg = "Ошибка: API-ключ Gemini не настроен. Пожалуйста, добавьте GEMINI_API_KEY в панели Secrets приложения."
            codexDao.insertMessage(CodexMessage(content = errMsg, isUser = false))
            return errMsg
        }

        // Build System Prompt setting up Codex Ubuntu NodeJS context
        val systemPrompt = "Вы являетесь портом OpenAI Codex Node.js, запущенным локально внутри Ubuntu PRoot на Android-устройстве.\n" +
                "У вас есть полный доступ к файловой системе через оболочку. Вы можете:\n" +
                "1. Создавать, редактировать и читать файлы на /sdcard/ или в текущей папке ~/ (виртуальный root).\n" +
                "2. Выполнять локальные unix-команды.\n" +
                "\n" +
                "ВАЖНОЕ ТРЕБОВАНИЕ К ВЫВОДУ:\n" +
                "Если для выполнения запроса пользователя нужно СОЗДАТЬ ФАЙЛ или ВЫПОЛНИТЬ КОМАНДУ, вы ОБЯЗАТЕЛЬНО должны добавить в самом конце вашего русскоязычного ответа специальный блок JSON-инструкций, обернутый в тройные обратные кавычки с тегом ```json-action.\n" +
                "Приложения автоматически распознает этот блок и физически выполнит действия на телефоне!\n" +
                "\n" +
                "Пример формата JSON-action:\n" +
                "```json-action\n" +
                "{\n" +
                "  \"write_file_path\": \"/sdcard/calc.py\",\n" +
                "  \"write_file_content\": \"print('hello')\",\n" +
                "  \"terminal_command_execute\": \"python /sdcard/calc.py\"\n" +
                "}\n" +
                "```\n" +
                "Если нужно просто выполнить команду без записи файла:\n" +
                "```json-action\n" +
                "{\n" +
                "  \"terminal_command_execute\": \"ls -la\"\n" +
                "}\n" +
                "```\n" +
                "Отвечайте пользователю на русском языке вежливо, уверенно и технически грамотно. Сначала объясняйте, что вы делаете, а в самом конце добавляйте json-action, если это применимо."

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Запрос пользователя: $prompt")))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = GeminiClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Не удалось получить ответ от Codex-сервера."

            // Save Codex answer
            codexDao.insertMessage(CodexMessage(content = responseText, isUser = false))

            // Check if response contains an actionable JSON instruction
            parseAndExecuteActions(responseText)

            return responseText
        } catch (e: Exception) {
            val errorMsg = "Сбой сервера Codex Daemon: ${e.message}"
            codexDao.insertMessage(CodexMessage(content = errorMsg, isUser = false))
            return errorMsg
        }
    }

    private suspend fun parseAndExecuteActions(text: String) {
        val tagStart = "```json-action"
        val tagEnd = "```"
        if (text.contains(tagStart)) {
            try {
                val startIndex = text.indexOf(tagStart) + tagStart.length
                val endIndex = text.indexOf(tagEnd, startIndex)
                if (endIndex > startIndex) {
                    val jsonStr = text.substring(startIndex, endIndex).trim()
                    val json = JSONObject(jsonStr)

                    // 1. Check for file write operation
                    if (json.has("write_file_path") && json.has("write_file_content")) {
                        val originalPath = json.getString("write_file_path")
                        val fileContent = json.getString("write_file_content")

                        // Resolve path safely: mapping /sdcard properly if permission granted
                        val resolvedPath = if (originalPath.startsWith("/sdcard")) {
                            originalPath.replace("/sdcard", android.os.Environment.getExternalStorageDirectory().absolutePath)
                        } else {
                            // Default to virtual Ubuntu Home
                            File(shellRunner.curDirectory, originalPath.removePrefix("~/").removePrefix("/root/")).absolutePath
                        }

                        val targetFile = File(resolvedPath)
                        targetFile.parentFile?.mkdirs()
                        targetFile.writeText(fileContent)

                        // Insert system note about file creation to Codex log
                        codexDao.insertMessage(
                            CodexMessage(
                                content = "✨ [Авто-Действие] Файл успешно создан по пути: $originalPath",
                                isUser = false,
                                isCommandResult = true
                            )
                        )
                    }

                    // 2. Check for shell command execution
                    if (json.has("terminal_command_execute")) {
                        val cmdToRun = json.getString("terminal_command_execute")
                        codexDao.insertMessage(
                            CodexMessage(
                                content = "⚙️ [Авто-Действие] Выполнение команды: $cmdToRun...",
                                isUser = false,
                                isCommandResult = true
                            )
                        )

                        // Execute it physically inside our ShellRunner!
                        val cmdOutput = executeTerminalCommand(cmdToRun)

                        // Insert command result
                        codexDao.insertMessage(
                            CodexMessage(
                                content = "📋 [Сводка Вывода]:\n$cmdOutput",
                                isUser = false,
                                isCommandResult = true,
                                commandExecuted = cmdToRun
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                codexDao.insertMessage(
                    CodexMessage(
                        content = "⚠️ [Действие завершилось ошибкой]: ${e.message}",
                        isUser = false,
                        isCommandResult = true
                    )
                )
            }
        }
    }
}
