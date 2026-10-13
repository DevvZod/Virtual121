package com.horcruxes.box.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.horcruxes.box.R
import top.niunaijun.blackbox.BlackBoxCore
import java.util.Locale

/**
 * Add-App: lists launchable apps on the device; tap to install into the
 * container. All engine calls run off the UI thread — install copies
 * gigabytes and per-row isInstalled hits binder IPC.
 */
class AppsActivity : Activity() {

    companion object {
        private const val TAG = "HORCRUX"
    }

    private val TEXT = Color.parseColor("#D6E5D6")
    private val DIM = Color.parseColor("#7A8F7E")
    private val GREEN = Color.parseColor("#33FF66")

    private val appsList by lazy { findViewById<LinearLayout>(R.id.appsList) }
    private var allApps: List<AppEntry> = emptyList()
    private var boundPackages: Set<String> = emptySet()

    private data class AppEntry(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apps)

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(
            object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    renderApps(s?.toString()?.trim().orEmpty())
                }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            }
        )
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        Thread {
            try {
                val pm = packageManager
                val launcherIntent = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                val apps = pm.queryIntentActivities(launcherIntent, 0)
                    .asSequence()
                    .map { it.activityInfo }
                    .map { ai ->
                        val label = try {
                            pm.getApplicationLabel(ai.applicationInfo).toString()
                        } catch (t: Throwable) { ai.packageName }
                        val icon = try {
                            pm.getApplicationIcon(ai.packageName)
                        } catch (t: Throwable) { null }
                        AppEntry(label, ai.packageName, icon)
                    }
                    .filter { it.packageName != packageName }
                    .distinctBy { it.packageName }
                    .sortedBy { it.label.lowercase(Locale.getDefault()) }
                    .toList()

                val bound = mutableSetOf<String>()
                val core = BlackBoxCore.get()
                for (a in apps) {
                    try {
                        if (core.isInstalled(a.packageName, 0)) bound.add(a.packageName)
                    } catch (t: Throwable) {
                        Log.w(TAG, "isInstalled failed for ${a.packageName}: ${t.message}")
                    }
                }

                allApps = apps
                boundPackages = bound
                runOnUiThread { renderApps(currentQuery()) }
            } catch (t: Throwable) {
                Log.e(TAG, "loadApps failed", t)
            }
        }.start()
    }

    private fun currentQuery(): String =
        findViewById<EditText>(R.id.searchInput)?.text?.toString()?.trim().orEmpty()

    private fun renderApps(query: String) {
        appsList.removeAllViews()
        val filtered = if (query.isEmpty()) allApps
        else allApps.filter {
            it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
        }

        if (filtered.isEmpty()) {
            appsList.addView(TextView(this).apply {
                text = "No souls found."
                textSize = 13f
                setTextColor(DIM)
                setPadding(dp(4), dp(10), dp(4), dp(10))
            })
            return
        }

        for (entry in filtered.take(400)) {
            appsList.addView(appRow(entry))
        }
    }

    private fun appRow(entry: AppEntry): View {
        val alreadyBound = boundPackages.contains(entry.packageName)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0C130D"))
                cornerRadius = dp(10).toFloat()
                setStroke(1, Color.parseColor("#14291B"))
            }
        }

        row.addView(ImageView(this).apply {
            setImageDrawable(entry.icon)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        })

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        textCol.addView(TextView(this).apply {
            text = entry.label
            textSize = 14f
            setTextColor(TEXT)
            setTypeface(typeface, Typeface.BOLD)
        })
        textCol.addView(TextView(this).apply {
            text = entry.packageName
            textSize = 11f
            setTextColor(DIM)
        })
        row.addView(textCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(this).apply {
            text = if (alreadyBound) "BOUND" else "BIND"
            textSize = 12f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(if (alreadyBound) DIM else GREEN)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        })

        row.setOnClickListener {
            // Pro: detect split installs + always offer a working action.
            // UX lesson: the previous build attached NO listener to BOUND
            // rows, so a re-bind was impossible from this screen and the
            // stale (split-less) clone kept being launched.
            val splits: Array<String>? = try {
                packageManager.getPackageInfo(entry.packageName, 0).splitNames
            } catch (t: Throwable) {
                null
            };
            val splitInfo = if (splits != null && splits.isNotEmpty())
                " (${splits.size} splits will be cloned: ${splits.joinToString(", ")})" else ""

            if (alreadyBound) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Already bound")
                    .setMessage(
                        "${entry.label} is already in the container.$splitInfo\n\n" +
                        "Re-bind = fresh install that refreshes the clone and copies " +
                        "ALL splits (required after clone fixes/updates)."
                    )
                    .setPositiveButton("Re-bind") { _, _ -> doBind(entry, true) }
                    .setNeutralButton("Release") { _, _ ->
                        Thread {
                            try {
                                BlackBoxCore.get().uninstallPackageAsUser(entry.packageName, 0)
                            } catch (t: Throwable) {
                                Log.e(TAG, "release failed", t)
                            }
                            runOnUiThread { loadApps() }
                        }.start()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else if (splits != null && splits.isNotEmpty()) {
                Log.w(TAG, "split install detected: ${entry.packageName} splits=${splits.toList()}")
                android.app.AlertDialog.Builder(this)
                    .setTitle("Split-APK app detected")
                    .setMessage(
                        "${entry.label} was installed from a split bundle " +
                        "(${splits.size} splits). HORCRUXES will clone the base APK AND " +
                        "all ${splits.size} splits into the container - the guest loads " +
                        "their code and assets via splitSourceDirs.\n\n" +
                        "Proceed with the bind?"
                    )
                    .setPositiveButton("Bind") { _, _ -> doBind(entry, false) }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                doBind(entry, false)
            }
        }

        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrap.addView(row)
        wrap.addView(View(this).apply { minimumHeight = dp(8) })
        return wrap
    }

    private fun doBind(entry: AppEntry, reinstall: Boolean) {
        Toast.makeText(
            this,
            if (reinstall) "Re-binding ${entry.label} (fresh, with splits)..." else "Binding ${entry.label}...",
            Toast.LENGTH_SHORT
        ).show()
        Thread {
            try {
                val core = BlackBoxCore.get()
                if (reinstall) {
                    try {
                        core.uninstallPackageAsUser(entry.packageName, 0)
                    } catch (t: Throwable) {
                        Log.w(TAG, "pre-rebind uninstall failed (continuing)", t)
                    }
                }
                val res = core.installPackageAsUser(entry.packageName, 0)
                runOnUiThread {
                    val msg = if (res.success) {
                        "Soul bound: ${entry.label}"
                    } else {
                        "Binding failed: ${res.msg ?: "Unknown engine error"}"
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    if (res.success) loadApps()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "bind failed for ${entry.packageName}", t)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Binding crashed: ${t.message} (see logcat)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
