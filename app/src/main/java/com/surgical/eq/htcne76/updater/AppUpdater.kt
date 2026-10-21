package com.surgical.eq.htcne76.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isNewer: Boolean
)

object AppUpdater {

    private const val TAG = "AppUpdater"
    private const val GITHUB_REPO = "nasar0/EQ-apk"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val _updateInfoFlow = MutableStateFlow<UpdateInfo?>(null)
    val updateInfoFlow: StateFlow<UpdateInfo?> = _updateInfoFlow.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    suspend fun checkForUpdates(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SurgicalAudioEQ-App")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val tagName = json.optString("tag_name", "")
                val releaseName = json.optString("name", tagName)
                val releaseNotes = json.optString("body", "Mejoras y correcciones automáticas.")

                // Buscar el activo APK dentro de los assets
                var downloadUrl = ""
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i)
                        val name = asset?.optString("name", "") ?: ""
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset?.optString("browser_download_url", "") ?: ""
                            break
                        }
                    }
                }

                if (downloadUrl.isEmpty()) {
                    downloadUrl = "https://github.com/$GITHUB_REPO/releases/latest/download/Surgical-Audio-EQ-HTCNE76.apk"
                }

                val currentVersion = getCurrentVersionName(context)
                val isNewer = isRemoteVersionNewer(tagName, currentVersion)

                val info = UpdateInfo(
                    tagName = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    isNewer = isNewer
                )

                _updateInfoFlow.value = if (isNewer) info else null
                return@withContext if (isNewer) info else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error comprobando actualizaciones: ${e.message}")
        }
        return@withContext null
    }

    private fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    private fun isRemoteVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.removePrefix("v").trim()
        val cleanCurrent = currentVersion.removePrefix("v").trim()
        if (cleanRemote.isEmpty()) return false
        return cleanRemote != cleanCurrent
    }

    suspend fun downloadAndInstall(context: Context, downloadUrl: String) = withContext(Dispatchers.IO) {
        _isDownloading.value = true
        _downloadProgress.value = 0

        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "SurgicalAudioEQ-App")
                instanceFollowRedirects = true
            }

            // Manejar redirecciones (GitHub releases redirige a AWS S3)
            var actualConnection = connection
            if (connection.responseCode in listOf(301, 302, 307)) {
                val newUrl = connection.getHeaderField("Location")
                actualConnection = (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "SurgicalAudioEQ-App")
                }
            }

            val totalBytes = actualConnection.contentLength
            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            actualConnection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = ((totalRead * 100) / totalBytes).toInt()
                            _downloadProgress.value = progress
                        }
                    }
                    output.flush()
                }
            }

            _isDownloading.value = false
            _downloadProgress.value = 100

            // Lanzar la instalación automática del paquete APK
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando o instalando actualización: ${e.message}")
            _isDownloading.value = false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando el instalador de paquetes de Android: ${e.message}")
        }
    }

    fun dismissUpdate() {
        _updateInfoFlow.value = null
    }
}
