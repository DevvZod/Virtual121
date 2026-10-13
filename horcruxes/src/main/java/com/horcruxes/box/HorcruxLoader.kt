package com.horcruxes.box

import android.app.Application
import android.content.Context
import android.util.Log
import com.horcruxes.box.injector.ModuleInjector
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.BActivityThread
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.io.File

/**
 * Engine bootstrap + guest lifecycle interception.
 *
 * The [ModuleInjector] is registered on [AppLifecycleCallback.beforeApplicationOnCreate]
 * — the exact seam NeverLose VirtualBox uses (g4/c.smali): the payload lands
 * in the Unity guest BEFORE any Unity thread exists, so there is no
 * injection race against the engine lifecycle (the class of crashes we
 * chased in the old remote-inject loader).
 */
object HorcruxLoader {

    private const val TAG = "HORCRUX"

    val injector = ModuleInjector

    fun doAttachBaseContext(context: Context) {
        try {
            BlackBoxCore.get().doAttachBaseContext(context, object : ClientConfiguration() {
                override fun getHostPackageName(): String = context.packageName

                override fun isHideRoot(): Boolean = true

                override fun isHideXposed(): Boolean = true

                // CRITICAL: keeps the :black engine-server process alive as a
                // FOREGROUND service. Samsung OneUI aggressively freezes
                // cached processes — a frozen :black means every binder call
                // (install/launch/PM queries) hits null → "bind failed" NPE
                // → retry storms (observed: JNI global ref table overflow,
                // 51200 leaked BlackManager callbacks). The foreground
                // daemon is exempt from the cached-app freezer.
                override fun isEnableDaemonService(): Boolean = true

                override fun requestInstallPackage(file: File?, userId: Int): Boolean {
                    Log.w(TAG, "requestInstallPackage: ${file?.absolutePath} (user $userId)")
                    return false
                }
            })
        } catch (t: Throwable) {
            Log.e(TAG, "engine attach failed", t)
        }
    }

    fun doOnCreate(context: Context) {
        try {
            BlackBoxCore.get().doCreate()
            BlackBoxCore.get().addAppLifecycleCallback(object : AppLifecycleCallback() {
                override fun beforeCreateApplication(
                    packageName: String?,
                    processName: String?,
                    context: Context?,
                    userId: Int
                ) {
                    Log.d(TAG, "beforeCreateApplication pkg=$packageName proc=$processName user=$userId")
                }

                override fun beforeApplicationOnCreate(
                    packageName: String?,
                    processName: String?,
                    application: Application?,
                    userId: Int
                ) {
                    Log.d(TAG, "beforeApplicationOnCreate pkg=$packageName proc=$processName user=$userId")
                    try {
                        injector.onGuestPreCreate(
                            application = application,
                            packageName = packageName ?: "unknown",
                            processName = processName ?: "",
                            userId = userId
                        )
                    } catch (t: Throwable) {
                        Log.e(TAG, "module injection failed (fail-open guest)", t)
                    }
                }

                override fun afterApplicationOnCreate(
                    packageName: String?,
                    processName: String?,
                    application: Application?,
                    userId: Int
                ) {
                    Log.d(TAG, "afterApplicationOnCreate pkg=$packageName proc=$processName")
                }
            })
            Log.i(TAG, "HORCRUXES engine ready (BActivityThread uid=${BActivityThread.getUserId()})")
        } catch (t: Throwable) {
            Log.e(TAG, "engine create failed", t)
        }
    }
}
