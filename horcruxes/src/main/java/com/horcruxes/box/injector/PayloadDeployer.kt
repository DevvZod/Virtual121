package com.horcruxes.box.injector

import android.app.Application
import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.core.env.BEnvironment
import java.io.File

// Payload logistics.
//
// Deploy is HOST-side: the payload lives in the HOST APK assets, and the
// host writes it into the guest's BEnvironment data dir (a real
// filesystem path). The guest then loads it by absolute path through
// BlackBox storage redirection (guest getFilesDir maps to the same dir).
//
// Deploying from inside the guest is impossible: the guest Application's
// assets belong to the GAME's apk, not ours.
object PayloadDeployer {

    private const val TAG = "HORCRUX-DEPLOY"
    private const val PAYLOAD_DIR = "horcrux"
    private const val PAYLOAD_NAME = "libhorcrux_payload.so"
    private const val DECOY_NAME = "libshadowhook_nothing.so"

    // HOST-side: guest data dir (real path under BEnvironment).
    fun guestDataFilesDir(packageName: String, userId: Int): File {
        val dir = File(BEnvironment.getDataFilesDir(packageName, userId), PAYLOAD_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // GUEST-side: decoy file location inside the guest's own filesDir.
    fun guestDecoyFile(application: Application): File {
        return File(File(application.filesDir, PAYLOAD_DIR), DECOY_NAME)
    }

    // HOST-side deploy: copy assets into the guest data dir. Called from
    // MainActivity before launchApk so the payload is always fresh.
    fun deployForGuest(context: Context, packageName: String, userId: Int): Boolean {
        return try {
            val dir = guestDataFilesDir(packageName, userId)

            try {
                val decoy = File(dir, DECOY_NAME)
                context.assets.open("horcrux/" + DECOY_NAME).use { input ->
                    decoy.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "decoy deploy skipped: " + t.message)
            }

            val out = File(dir, PAYLOAD_NAME)
            context.assets.open("horcrux/" + PAYLOAD_NAME).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "deployed " + out.length() + " bytes to " + out.absolutePath + " for " + packageName)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "payload deploy failed for " + packageName, t)
            false
        }
    }

    // GUEST-side: resolve the payload inside the guest's own filesDir.
    fun locateDeployedPayload(application: Application, packageName: String): File? {
        val f = File(File(application.filesDir, PAYLOAD_DIR), PAYLOAD_NAME)
        return if (f.exists() && f.length() > 0) f else null
    }

    /**
     * HOST-side: pre-create the guest's whole data tree (internal + the
     * /sdcard-mapped external dir) BEFORE launch.
     *
     * Games like MLBB stat their data dir for free space the moment they
     * boot ("not enough space" guard). If the container's external dir
     * does not exist yet, statfs fails → 0 bytes → the guard trips even on
     * a device with plenty of space. Pre-creating the tree makes the
     * first stat succeed; usableSpace below is our diagnostic.
     */
    fun prepareGuestStorage(packageName: String, userId: Int): Long {
        return try {
            val internal = BEnvironment.getDataDir(packageName, userId)
            for (sub in arrayOf("", "files", "cache", "app_libs", "databases")) {
                val f = if (sub.isEmpty()) internal else File(internal, sub)
                if (!f.exists()) f.mkdirs()
            }

            val external = BEnvironment.getExternalDataDir(packageName, userId)
            for (sub in arrayOf("", "files", "cache")) {
                val f = if (sub.isEmpty()) external else File(external, sub)
                if (!f.exists()) f.mkdirs()
            }

            val internalUsable = internal.usableSpace
            val externalUsable = external.usableSpace
            Log.i(TAG, "storage prep: internal=${internal.absolutePath} exists=${internal.exists()} usable=$internalUsable")
            Log.i(TAG, "storage prep: external=${external.absolutePath} exists=${external.exists()} usable=$externalUsable")
            externalUsable
        } catch (t: Throwable) {
            Log.e(TAG, "storage prep failed", t)
            -1L
        }
    }
}
