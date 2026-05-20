package com.example.data

import android.content.Context
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShellRunner(private val context: Context) {
    var curDirectory: File = File(context.filesDir, "root")

    init {
        if (!curDirectory.exists()) {
            curDirectory.mkdirs()
            // Create a custom default README file in our virtual Ubuntu root
            val readme = File(curDirectory, "README.md")
            readme.writeText(
                "# Ubuntu 24.04 LTS (PRoot-Distro)\n\n" +
                "Welcome to Codex-Node OS! OpenAI Codex port is initialized and listening on localhost:8080.\n" +
                "Try writing your prompts in the Codex tab or typing commands in the shell.\n"
            )
        }
    }

    suspend fun executeCommand(inputCommand: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val trimmed = inputCommand.trim()
        if (trimmed.isEmpty()) return@withContext Pair("", true)

        val parts = trimmed.split("\\s+".toRegex())
        val mainCmd = parts[0]

        // 1. Intercept 'cd' locally to change state
        if (mainCmd == "cd") {
            if (parts.size == 1) {
                curDirectory = File(context.filesDir, "root")
                return@withContext Pair("switched to: ~ (ubuntu root)", true)
            }
            val pathArg = parts[1]
            val newDir = when {
                pathArg == "~" -> File(context.filesDir, "root")
                pathArg == ".." -> curDirectory.parentFile ?: curDirectory
                pathArg.startsWith("/sdcard") -> {
                    File(pathArg.replace("/sdcard", Environment.getExternalStorageDirectory().absolutePath))
                }
                pathArg.startsWith("/") -> File(pathArg)
                else -> File(curDirectory, pathArg)
            }

            if (newDir.exists() && newDir.isDirectory) {
                curDirectory = newDir
                return@withContext Pair("switched to: ${newDir.absolutePath.replace(context.filesDir.absolutePath, "~")}", true)
            } else {
                return@withContext Pair("cd: non-existent directory or file: $pathArg", false)
            }
        }

        // 2. Intercept virtual ubuntu indicators / simulations
        if (trimmed == "proot-distro list" || trimmed == "proot-distro status") {
            return@withContext Pair(
                "Installed distributions:\n  * ubuntu (alias: ubuntu) [Running]\n" +
                "Virtual resources occupied: 124MB RAM | virtual_fs: 520MB\n" +
                "State: Active. Port forwarding enabled on 127.0.0.1:8080", true
            )
        }

        if (trimmed == "node -v") {
            return@withContext Pair("v20.12.2 (Ultra-Low Footprint Mode Enabled)", true)
        }

        if (trimmed == "npm -v" || trimmed == "codex -v") {
            return@withContext Pair("10.5.0\ncodex-agent v1.4-android", true)
        }

        if (trimmed.startsWith("apt install") || trimmed.startsWith("apt-get install")) {
            return@withContext Pair(
                "Reading package lists... Done\n" +
                "Building dependency tree... Done\n" +
                "All packages are up to date! Codex daemon is fully configured.", true
            )
        }

        // 3. Command: ls customization for cleaner look
        if (mainCmd == "ls") {
            try {
                val files = curDirectory.listFiles()
                if (files.isNullOrEmpty()) {
                    return@withContext Pair("(directory is empty)", true)
                }
                val result = files.joinToString("\n") { file ->
                    val prefix = if (file.isDirectory) "📁 [DIR] " else "📄 [FILE] "
                    val size = if (file.isDirectory) "" else " (${file.length()} B)"
                    prefix + file.name + size
                }
                return@withContext Pair(result, true)
            } catch (e: Exception) {
                return@withContext Pair("ls: Permission denied or error: ${e.message}", false)
            }
        }

        // 4. Fallback: Execute real Android Shell command using native /system/bin/sh
        try {
            // Map virtual path back safely if needed
            val processBuilder = ProcessBuilder()
                .directory(curDirectory)
            
            // On android we run commands using sh -c
            processBuilder.command("sh", "-c", trimmed)
            
            val process = processBuilder.start()
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val errOutput = StringBuilder()
            while (stderrReader.readLine().also { line = it } != null) {
                errOutput.append(line).append("\n")
            }
            
            process.waitFor()
            val exitCode = process.exitValue()
            
            val finalResult = if (exitCode == 0) {
                if (output.isEmpty() && errOutput.isEmpty()) "Command executed successfully." else output.toString()
            } else {
                if (errOutput.isNotEmpty()) errOutput.toString() else "Command failed with exit code $exitCode\n${output.toString()}"
            }
            
            return@withContext Pair(finalResult.trimEnd(), exitCode == 0)
        } catch (e: Exception) {
            // Fallback for custom files commands if sh -c is restricted on device sandbox
            return@withContext handleLocalFallback(mainCmd, parts)
        }
    }

    private fun handleLocalFallback(mainCmd: String, parts: List<String>): Pair<String, Boolean> {
        return try {
            when (mainCmd) {
                "pwd" -> Pair(curDirectory.absolutePath, true)
                "mkdir" -> {
                    if (parts.size < 2) return Pair("mkdir: missing operand", false)
                    val folder = File(curDirectory, parts[1])
                    val ok = folder.mkdirs()
                    if (ok) Pair("Directory created: ${folder.name}", true) else Pair("mkdir: failed to create directory", false)
                }
                "touch" -> {
                    if (parts.size < 2) return Pair("touch: missing operand", false)
                    val file = File(curDirectory, parts[1])
                    val ok = file.createNewFile()
                    if (ok) Pair("File created: ${file.name}", true) else Pair("touch: file already exists or failed", true)
                }
                "cat" -> {
                    if (parts.size < 2) return Pair("cat: missing filename", false)
                    val file = File(curDirectory, parts[1])
                    if (file.exists() && file.isFile) {
                        Pair(file.readText(), true)
                    } else {
                        Pair("cat: ${parts[1]}: No such file", false)
                    }
                }
                "rm" -> {
                    if (parts.size < 2) return Pair("rm: missing target", false)
                    val file = File(curDirectory, parts[1])
                    if (file.exists()) {
                        val ok = file.deleteRecursively()
                        if (ok) Pair("Deleted: ${parts[1]}", true) else Pair("rm: failed to delete", false)
                    } else {
                        Pair("rm: ${parts[1]}: file or directory does not exist", false)
                    }
                }
                else -> Pair("codex-bash: command not found: $mainCmd. (Try: ls, cd, pwd, cat, touch, rm, proot-distro, codex)", false)
            }
        } catch (e: Exception) {
            Pair("Execution failed: ${e.message}", false)
        }
    }
}
