package com.horcruxes.box.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.horcruxes.box.R
import com.horcruxes.box.injector.PayloadDeployer
import top.niunaijun.blackbox.BlackBoxCore

class MainActivity : Activity() {

    companion object {
        private const val TAG = "HORCRUX"
        private const val PERMISSION_REQUEST = 0x7EA
        private const val INSTALL_REQUEST = 0x1337
    }

    private val TEXT = Color.parseColor("#D6E5D6")
    private val DIM = Color.parseColor("#7A8F7E")
    private val GREEN = Color.parseColor("#33FF66")

    private val soulsList by lazy { findViewById<LinearLayout>(R.id.soulsList) }

    private val gamePackages = listOf("com.mobile.legends")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnBindInstalled).setOnClickListener {
            startActivity(Intent(this, AppsActivity::class.java))
        }
        findViewById<Button>(R.id.btnInstallApk).setOnClickListener { pickApk() }
        findViewById<Button>(R.id.btnModules).setOnClickListener {
            startActivity(Intent(this, ModulesActivity::class.java))
        }

        findViewById<Button>(R.id.btnStorage).setOnClickListener { openStorageAccess() }


        ensureStoragePermission()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /**
     * The engine writes its virtual root to /sdcard/AkatskiGamespace
     * (legacy storage design, requires targetSdk < 30 + this runtime grant
     * on Android 6+). Without it the first install/bind crashes.
     */
    private fun ensureStoragePermission() {
        if (Build.VERSION.SDK_INT < 23) return
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (needed.isEmpty()) return
        ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERMISSION_REQUEST)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST) return
        val denied = grantResults.any { it != PackageManager.PERMISSION_GRANTED }
        if (denied) {
            Toast.makeText(
                this,
                "Storage permission denied — binding apps will fail on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // All engine ops off the UI thread: installs copy gigabytes of APK data.
    private fun bg(block: () -> Unit) {
        Thread {
            try {
                block()
            } catch (t: Throwable) {
                Log.e(TAG, "engine op failed", t)
                runOnUiThread {
                    Toast.makeText(this, "Engine error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun ui(block: () -> Unit) = runOnUiThread { block() }

    private fun refresh() {
        Thread {
            try {
                val rows = mutableListOf<Pair<String, String>>()
                val core = BlackBoxCore.get()
                // FIX: list EVERY app bound in the container (user 0), not just
                // the hardcoded game package, so apps added via "Add App" show up.
                val bound = core.getInstalledApplications(0, 0) ?: emptyList()
                for (info in bound) {
                    val pkg = info.packageName ?: continue
                    if (pkg == packageName) continue
                    rows.add(labelFor(info) to pkg)
                }
                rows.sortBy { it.first.lowercase() }
                ui { renderSouls(rows) }
            } catch (t: Throwable) {
                Log.e(TAG, "refresh failed", t)
            }
        }.start()
    }

    // Read label from host packageManager if installed on host,
    // otherwise parse label directly from the container's copy of the APK.
    private fun labelFor(info: ApplicationInfo): String {
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(info.packageName, 0)
            ).toString()
        } catch (t: Throwable) {
            try {
                val archive = packageManager.getPackageArchiveInfo(info.sourceDir, 0)?.applicationInfo
                if (archive != null) {
                    archive.sourceDir = info.sourceDir
                    archive.publicSourceDir = info.sourceDir
                    archive.loadLabel(packageManager).toString()
                } else info.packageName
            } catch (t2: Throwable) {
                info.packageName
            }
        }
    }

    private fun renderSouls(rows: List<Pair<String, String>>) {
        soulsList.removeAllViews()
        if (rows.isEmpty()) {
            soulsList.addView(TextView(this).apply {
                text = "No souls bound yet.\nUse ADD APP above to bind the game."
                textSize = 13f
                setTextColor(DIM)
                setLineSpacing(0f, 1.3f)
            })
            return
        }
        for ((name, pkg) in rows) {
            soulsList.addView(appRow(name, pkg))
        }
    }

    private fun appRow(name: String, pkg: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0C130D"))
                cornerRadius = dp(10).toFloat()
                setStroke(1, Color.parseColor("#14291B"))
            }
        }
        val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(TextView(this).apply {
            text = name
            textSize = 15f
            setTextColor(TEXT)
            setTypeface(typeface, Typeface.BOLD)
        })
        textCol.addView(TextView(this).apply {
            text = pkg
            textSize = 11f
            setTextColor(DIM)
        })
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val launch = Button(this).apply {
            text = "LAUNCH"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(GREEN)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1F5233"))
                cornerRadius = dp(8).toFloat()
            }
            setPadding(dp(14), dp(6), dp(14), dp(6))
        }
        launch.setOnClickListener { launchGame(pkg) }
        row.addView(launch)

        row.setOnLongClickListener {
            confirmRelease(pkg)
            true
        }

        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        outer.addView(row)
        outer.addView(View(this).apply { minimumHeight = dp(10) })
        return outer
    }

    /** Android 11+: "All files access" is a user-toggled special grant —
     * MANAGE_EXTERNAL_STORAGE must be declared (done) + the user flips the
     * switch. This shows state and opens the grant screen directly. */
    private fun openStorageAccess() {
        // isExternalStorageManager() / ALL_FILES_ACCESS only exist on API 30+;
        // calling them on Android 10 or lower would crash.
        val granted = if (Build.VERSION.SDK_INT >= 30)
            android.os.Environment.isExternalStorageManager() else true
        if (granted) {
            Toast.makeText(this, "All files access: GRANTED", Toast.LENGTH_SHORT).show()
            // Pre-create the venv tree now so /sdcard/AkatskiGamespace exists
            Thread { PayloadDeployer.prepareGuestStorage("com.mobile.legends", 0) }.start()
            return
        }
        Toast.makeText(this, "Grant All files access to HORCRUXES, then return.", Toast.LENGTH_LONG).show()
        try {
            startActivity(Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                android.net.Uri.parse("package:com.horcruxes.box")
            ))
        } catch (t: Throwable) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            } catch (t2: Throwable) {
                Toast.makeText(this, "Settings screen unavailable on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchGame(pkg: String) {
        Toast.makeText(this, "Raising the soul…", Toast.LENGTH_SHORT).show()
        bg {
            // Deploy the payload into the guest dir BEFORE launch so the
            // guest injector finds it at beforeApplicationOnCreate.
            try {
                PayloadDeployer.deployForGuest(this, pkg, 0)
            } catch (t: Throwable) {
                Log.e(TAG, "payload deploy failed", t)
            }
            // Pre-create the guest storage tree so the game's "free space"
            // guard sees a valid, stat-able dir instead of 0 bytes.
            val externalUsable = try {
                PayloadDeployer.prepareGuestStorage(pkg, 0)
            } catch (t: Throwable) {
                Log.e(TAG, "storage prep failed", t)
                -1L
            }

            if (externalUsable == 0L) {
                ui {
                    Toast.makeText(this, "External storage blocked - grant All files access to HORCRUXES", Toast.LENGTH_LONG).show()
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.parse("package:com.horcruxes.box"))
                        startActivity(intent)
                    } catch (t: Throwable) {
                        try {
                            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        } catch (t2: Throwable) {
                            Log.e(TAG, "all-files-access settings unavailable", t2)
                        }
                    }
                }
                return@bg
            }

            val ok = BlackBoxCore.get().launchApk(pkg, 0)
            ui {
                if (!ok) Toast.makeText(this, "The soul refused to rise.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmRelease(pkg: String) {
        AlertDialog.Builder(this)
            .setTitle("Release soul?")
            .setMessage("Uninstall $pkg from the container?")
            .setPositiveButton("Release") { _, _ ->
                bg {
                    BlackBoxCore.get().uninstallPackageAsUser(pkg, 0)
                    ui {
                        Toast.makeText(this, "Soul released.", Toast.LENGTH_SHORT).show()
                        refresh()
                    }
                }
            }
            .setNegativeButton("Keep", null)
            .show()
    }

    private fun pickApk() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/vnd.android.package-archive"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(
                Intent.createChooser(intent, "Bind an APK"), INSTALL_REQUEST)
        } catch (t: Throwable) {
            Toast.makeText(this, "No file picker available.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != INSTALL_REQUEST || resultCode != RESULT_OK || data?.data == null) return
        val uri: Uri = data.data!!
        Toast.makeText(this, "Binding soul…", Toast.LENGTH_SHORT).show()
        bg {
            val res = try {
                BlackBoxCore.get().installPackageAsUser(uri, 0)
            } catch (t: Throwable) {
                Log.e(TAG, "install from apk failed", t)
                null
            }
            ui {
                val msg = when {
                    res == null -> "Binding failed: engine error (see logcat)"
                    res.success -> "Soul bound: ${res.packageName}"
                    else -> "Binding failed: ${res.msg}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                refresh()
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
