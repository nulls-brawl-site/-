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

        // For all other commands, attempt real execution using ProcessBuilder
        // Check if our Ubuntu PRoot is installed
        val prootBin = File(context.filesDir, "proot")
        val rootFsDir = File(context.filesDir, "ubuntu_rootfs")
        val useProot = prootBin.exists() && rootFsDir.exists()

        try {
            val processBuilder = ProcessBuilder()
                .directory(curDirectory)
            
            val env = processBuilder.environment()
            env["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
            env["HOME"] = "/root"

            if (useProot) {
                // Real PRoot execution wrapper mapping Android files to /
                val commandList = listOf(
                    prootBin.absolutePath,
                    "--link2symlink",
                    "-0", // act as root
                    "-r", rootFsDir.absolutePath,
                    "-b", "/dev",
                    "-b", "/proc",
                    "-b", "${context.filesDir.absolutePath}:/root", // Mount app files at /root
                    "-w", curDirectory.absolutePath.replace(context.filesDir.absolutePath, "/root"),
                    "/usr/bin/env", "bash", "-c", trimmed
                )
                processBuilder.command(commandList)
            } else {
                // Standard Android Shell fallback if PRoot isn't set up yet
                processBuilder.command("sh", "-c", trimmed)
            }
            
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val outputStr = process.inputStream.bufferedReader().use { it.readText() }
            
            val exitCode = process.waitFor()
            
            val finalResult = if (exitCode == 0) {
                if (outputStr.isEmpty()) "Command executed successfully." else outputStr
            } else {
                if (outputStr.isEmpty()) "Command failed with exit code $exitCode" else outputStr
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
