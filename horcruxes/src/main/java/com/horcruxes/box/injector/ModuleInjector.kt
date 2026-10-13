package com.horcruxes.box.injector

import android.app.Application
import android.util.Log
import top.niunaijun.blackbox.core.env.BEnvironment
import java.io.File

/**
 * The Horcrux Injector — runs INSIDE the guest at
 * [AppLifecycleCallback.beforeApplicationOnCreate], before any guest
 * thread exists (deterministic — the remote-inject race class is
 * architecturally impossible here).
 *
 * Payload location is CONTEXT-FREE: BEnvironment computes the same real
 * filesystem dir the host deployed into, so a null/partially-initialized
 * guest Application can never block injection.
 */
object ModuleInjector {

    private const val TAG = "HORCRUX-INJ"

    /** Process-name fragments that host the game's Unity runtime. */
    private val UNITY_PROCESS_HINTS = arrayOf("UnityKillsMe", ":unity", ":p0", "unity")

    /** Packages we are provisioned to soul-bind. */
    private val TARGET_PACKAGES = setOf("com.mobile.legends")

    private val injected = HashSet<String>()

    fun onGuestPreCreate(
        application: Application?,
        packageName: String,
        processName: String,
        userId: Int
    ) {
        Log.i(TAG, "guest pre-create: pkg=$packageName proc=$processName user=$userId app=${application != null}")

        val isTarget =
            UNITY_PROCESS_HINTS.any { processName.contains(it, ignoreCase = true) } ||
                    TARGET_PACKAGES.contains(packageName)
        if (!isTarget) {
            Log.d(TAG, "skip non-unity guest: $packageName:$processName")
            return
        }

        val key = "$packageName:$processName:$userId"
        if (!injected.add(key)) {
            Log.d(TAG, "bootstrap already loaded for $key")
            return
        }

        // Context-free locate — BEnvironment works even when the guest
        // Application is null/partial on some engine builds.
        val dir = File(BEnvironment.getDataFilesDir(packageName, userId), "horcrux")
        val payload = File(dir, "libhorcrux_payload.so")
        val decoy = File(dir, "libshadowhook_nothing.so")
        Log.i(TAG, "payload path=${payload.absolutePath} exists=${payload.exists()} size=${if (payload.exists()) payload.length() else -1}")

        if (!payload.exists() || payload.length() == 0L) {
            Log.e(TAG, "payload not deployed for $key — host must deploy before launchApk")
            return
        }

        try {
            // Decoy first: the payload dlopens "libshadowhook_nothing.so"
            // by soname; a prior absolute-path System.load makes that
            // soname resolve to this instance.
            if (decoy.exists() && decoy.length() > 0) {
                try {
                    System.load(decoy.absolutePath)
                    Log.i(TAG, "decoy loaded")
                } catch (dt: Throwable) {
                    Log.w(TAG, "decoy load skipped: ${dt.message}")
                }
            } else {
                Log.w(TAG, "decoy missing (non-fatal)")
            }

            Log.i(TAG, "soul-binding $key -> ${payload.absolutePath} (${payload.length()} bytes)")
            System.load(payload.absolutePath)
            Log.i(TAG, "HORCRUX payload alive inside $key")
        } catch (t: Throwable) {
            // Fail-open: the guest keeps running unmodified.
            Log.e(TAG, "payload load failed for $key (fail-open)", t)
        }
    }
}
