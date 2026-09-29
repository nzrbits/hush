package com.nzrbits.hush.feature.updates

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.nzrbits.hush.core.common.AppDispatchers
import com.nzrbits.hush.core.common.BuildInfo
import com.nzrbits.hush.core.common.time.HushClock
import com.nzrbits.hush.core.datastore.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(
    val version: String,
    val tag: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val notes: String,
    val htmlUrl: String,
)

/** The whole update lifecycle as one state, shown as a single line on the home screen. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val file: File) : UpdateState
    data class Installing(val release: ReleaseInfo) : UpdateState
    data class NeedsInstallPermission(val release: ReleaseInfo, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Checks GitHub releases, downloads the APK into the cache directory and hands it to
 * PackageInstaller. Session installs let Android skip the confirmation on 12+ once Hush is
 * the installer of record for itself (first update still asks). No library, no analytics.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val clock: HushClock,
    private val dispatchers: AppDispatchers,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    val currentVersion: String get() = BuildInfo.normalize(BuildInfo.versionName)

    /** Queries the latest release. Returns the release when it is newer than the running build. */
    suspend fun check(force: Boolean = false): ReleaseInfo? = withContext(dispatchers.io) {
        val current = settings.current().updates
        if (!force && !current.autoCheck) return@withContext null
        if (!force && clock.now().toEpochMilli() - current.lastCheckMillis < MIN_INTERVAL_MILLIS) {
            return@withContext (state.value as? UpdateState.Available)?.release
        }
        _state.value = UpdateState.Checking
        val release = runCatching { fetchLatest() }.onFailure { Log.w(TAG, "check failed", it) }.getOrNull()
        settings.updateUpdates { it.copy(lastCheckMillis = clock.now().toEpochMilli()) }
        when {
            release == null -> { _state.value = UpdateState.Failed("Keine Verbindung zu GitHub."); null }
            BuildInfo.compare(release.version, currentVersion) > 0 && release.version != current.skippedVersion -> {
                _state.value = UpdateState.Available(release); release
            }
            else -> { _state.value = UpdateState.UpToDate; null }
        }
    }

    private fun fetchLatest(): ReleaseInfo? {
        val conn = (URL(API_LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Hush/${BuildInfo.versionName}")
        }
        try {
            if (conn.responseCode != 200) return null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name")
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl: String? = null
            var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) { apkUrl = a.optString("browser_download_url"); size = a.optLong("size"); break }
            }
            return ReleaseInfo(
                version = BuildInfo.normalize(tag),
                tag = tag,
                apkUrl = apkUrl ?: return null,
                apkSizeBytes = size,
                notes = json.optString("body"),
                htmlUrl = json.optString("html_url"),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads into cache; progress goes to [state]. */
    suspend fun download(release: ReleaseInfo): File? = withContext(dispatchers.io) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { if (it.name != "hush-${release.version}.apk") it.delete() }
        val target = File(dir, "hush-${release.version}.apk")
        if (target.exists() && (release.apkSizeBytes == 0L || target.length() == release.apkSizeBytes)) {
            _state.value = UpdateState.ReadyToInstall(release, target); return@withContext target
        }
        _state.value = UpdateState.Downloading(release, 0f)
        val ok = runCatching {
            var url = URL(release.apkUrl)
            var conn: HttpURLConnection
            var hops = 0
            while (true) {
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Hush/${BuildInfo.versionName}")
                }
                val code = conn.responseCode
                if (code in 300..399 && hops < 5) {
                    val loc = conn.getHeaderField("Location") ?: error("redirect without location")
                    conn.disconnect(); url = URL(url, loc); hops++; continue
                }
                if (code != 200) error("HTTP $code")
                break
            }
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } >= 0) {
                        out.write(buf, 0, read)
                        done += read
                        if (total > 0) _state.value = UpdateState.Downloading(release, (done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            conn.disconnect()
        }.onFailure { Log.w(TAG, "download failed", it); target.delete() }.isSuccess
        if (!ok) { _state.value = UpdateState.Failed("Download abgebrochen."); return@withContext null }
        _state.value = UpdateState.ReadyToInstall(release, target)
        target
    }

    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * Hands the APK to PackageInstaller. With USER_ACTION_NOT_REQUIRED Android 12+ installs
     * silently when Hush is the installer of record and the signature matches; otherwise the
     * receiver gets PENDING_USER_ACTION and shows the system dialog.
     */
    suspend fun install(release: ReleaseInfo, file: File) = withContext(dispatchers.io) {
        if (!canInstall()) { _state.value = UpdateState.NeedsInstallPermission(release, file); return@withContext }
        _state.value = UpdateState.Installing(release)
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(file.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setPackageSource(PackageInstaller.PACKAGE_SOURCE_OTHER)
                }
            }
            val sessionId = installer.createSession(params)
            installer.openSession(sessionId).use { session ->
                session.openWrite("hush.apk", 0, file.length()).use { out ->
                    file.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
                val intent = Intent(context, UpdateReceiver::class.java).setAction(UpdateReceiver.ACTION_RESULT)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
                session.commit(pending.intentSender)
            }
        }.onFailure {
            Log.w(TAG, "install failed", it)
            _state.value = UpdateState.Failed("Installation fehlgeschlagen: ${it.message}")
        }
    }

    /** Called by [UpdateReceiver]. */
    fun onInstallResult(status: Int, message: String?) {
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> _state.value = UpdateState.UpToDate
            PackageInstaller.STATUS_PENDING_USER_ACTION -> Unit // dialog shown by the receiver
            else -> _state.value = UpdateState.Failed(message ?: "Installation abgebrochen.")
        }
    }

    suspend fun skip(version: String) {
        settings.updateUpdates { it.copy(skippedVersion = version) }
        _state.value = UpdateState.Idle
    }

    fun dismissError() { if (state.value is UpdateState.Failed) _state.value = UpdateState.Idle }

    companion object {
        private const val TAG = "HushUpdate"
        const val API_LATEST = "https://api.github.com/repos/nzrbits/hush/releases/latest"
        const val MIN_INTERVAL_MILLIS = 6L * 60 * 60 * 1000
    }
}
