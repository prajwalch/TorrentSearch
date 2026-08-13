package com.prajwalch.torrentsearch

import android.content.Context
import android.content.pm.ApplicationInfo

import androidx.core.content.pm.PackageInfoCompat

import com.prajwalch.torrentsearch.constant.TorrentSearchConstants

/**
 * Runtime app metadata for code that lives in the `:app` Android library.
 *
 * The AGP Kotlin Multiplatform Android library plugin does not generate
 * `BuildConfig`, so the application module initializes this once at startup.
 */
object AppInfo {
    lateinit var packageName: String
        private set

    lateinit var versionName: String
        private set

    var versionCode: Long = 0
        private set

    /** True when the installed app is debuggable (debug build / debugger attachable). */
    var isDebuggable: Boolean = false
        private set

    val githubReleaseUrl: String
        get() = "${TorrentSearchConstants.GITHUB_REPO_URL}/releases/tag/v$versionName"

    fun init(context: Context) {
        packageName = context.packageName
        isDebuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        versionName = packageInfo.versionName ?: "0.0.0"
        versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    }
}
