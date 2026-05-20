package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class EnvironmentInstaller(private val context: Context) {

    private val _installProgress = MutableStateFlow(0f)
    val installProgress: StateFlow<Float> = _installProgress

    private val _installStatus = MutableStateFlow("Ожидание установки...")
    val installStatus: StateFlow<String> = _installStatus

    // Реальные ссылки для aarch64 (arm64)
    private val staticProotUrl = "https://github.com/termux/proot/releases/download/v5.3.1/proot-v5.3.1-aarch64-static"
    private val ubuntuRootfsUrl = "https://cdimage.ubuntu.com/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.5-base-arm64.tar.gz"

    suspend fun installEnvironment(): Boolean = withContext(Dispatchers.IO) {
        val ubuntuDir = File(context.filesDir, "ubuntu_rootfs")
        val prootBin = File(context.filesDir, "proot")

        // Проверка: уже установлено?
        if (ubuntuDir.exists() && File(ubuntuDir, "bin/bash").exists() && prootBin.exists()) {
            _installStatus.value = "Среда (Ubuntu + PRoot) уже установлена"
            _installProgress.value = 1f
            return@withContext true
        }

        try {
            ubuntuDir.mkdirs()

            // 1. Download Static PRoot
            if (!prootBin.exists() || prootBin.length() == 0L) {
                _installStatus.value = "Скачивание статического PRoot..."
                _installProgress.value = 0.05f
                downloadFile(staticProotUrl, prootBin) { progress ->
                    _installProgress.value = 0.05f + (progress * 0.10f)
                }
                prootBin.setExecutable(true, false)
            }

            // 2. Download Ubuntu Base RootFS
            val tarFile = File(context.cacheDir, "ubuntu-rootfs.tar.gz")
            if (!tarFile.exists() || tarFile.length() < 1000L) { // basic check
                _installStatus.value = "Скачивание Ubuntu 22.04 RootFS (~30MB)..."
                downloadFile(ubuntuRootfsUrl, tarFile) { progress ->
                    _installProgress.value = 0.15f + (progress * 0.45f) // up to 60%
                }
            }

            // 3. Extract RootFS via Android's native tar wrapper (toybox)
            if (!File(ubuntuDir, "bin/bash").exists()) {
                _installStatus.value = "Распаковка Ubuntu RootFS (подождите, это займет время)..."
                _installProgress.value = 0.7f
                extractTarGzNative(tarFile, ubuntuDir)
                _installProgress.value = 0.9f
            }

            // 4. Cleanup
            tarFile.delete()
            _installStatus.value = "Создание среды завершено!"
            _installProgress.value = 1f
            return@withContext true

        } catch (e: Exception) {
            Log.e("Installer", "Environment Install Failed", e)
            _installStatus.value = "Ошибка: ${e.message}"
            return@withContext false
        }
    }

    private suspend fun extractTarGzNative(tarGzFile: File, destDir: File) {
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", destDir.absolutePath)
                .redirectErrorStream(true)
                .start()
                
            // Conserve output buffer to prevent Process block
            var outText = ""
            process.inputStream.bufferedReader().use { reader ->
                outText = reader.readText()
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw Exception("Отказ tar распаковщика. Exit: $exitCode, Log: $outText")
            }
        }
    }

    private suspend fun downloadFile(urlString: String, outputFile: File, onProgress: (Float) -> Unit) {
        withContext(Dispatchers.IO) {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()

            // If GitHub redirected
            val responseCode = connection.responseCode
            val inputStream: InputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                throw Exception("Server returned HTTP $responseCode")
            }

            val fileLength = connection.contentLength

            inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    var lastUpdate = System.currentTimeMillis()
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 250) { // Limit UI updates to 4fps
                                onProgress(total.toFloat() / fileLength.toFloat())
                                lastUpdate = now
                            }
                        }
                    }
                    if (fileLength > 0) {
                        onProgress(1f)
                    }
                }
            }
        }
    }
}
