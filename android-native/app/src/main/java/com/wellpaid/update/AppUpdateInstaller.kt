package com.wellpaid.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object AppUpdateInstaller {

    fun canInstallFromUnknownSources(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallFromUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent =
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    /** Destino do APK em cache; substituído a cada download. */
    fun updateApkFile(context: Context): File =
        File(context.cacheDir, "apk_updates/wellpaid-update.apk").also {
            it.parentFile?.mkdirs()
        }

    suspend fun downloadApkToFile(
        client: OkHttpClient,
        url: String,
        dest: File,
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("HTTP ${response.code}")
            }
            val body = response.body ?: error("empty body")
            body.byteStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    /**
     * Abre o instalador do sistema para o APK descarregado.
     * @return null em caso de sucesso, ou mensagem de erro curta.
     */
    fun startSystemInstaller(context: Context, apkFile: File): String? {
        if (!apkFile.isFile || apkFile.length() == 0L) {
            return "APK file missing."
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallFromUnknownSources(context)) {
            return "permission"
        }
        val uri =
            try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile,
                )
            } catch (e: Exception) {
                return e.message ?: "FileProvider error"
            }
        return try {
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val pm: PackageManager = context.packageManager
            if (intent.resolveActivity(pm) == null) {
                return "No app to handle install."
            }
            context.startActivity(intent)
            null
        } catch (e: Exception) {
            e.message ?: "Install failed"
        }
    }
}
