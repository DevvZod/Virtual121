package com.horcruxes.box.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Horcrux vault state: auth session + module switches.
 *
 * Server glue (Phase 3) plugs in behind this facade — same model as the
 * audited NeverLose: LoginRequest → AuthSession, ModulesResponse →
 * BinaryModule(downloadUrl, sha256, packageNames, enabled).
 * The existing mod-menu key server already speaks this shape.
 */
object HorcruxRepository {

    private const val PREFS = "horcrux_vault"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Auth session ──
    fun saveSession(ctx: Context, key: String, expiryMs: Long, tier: String) {
        prefs(ctx).edit()
            .putString("key", key)
            .putLong("expiry", expiryMs)
            .putString("tier", tier)
            .apply()
    }

    fun isSessionValid(ctx: Context): Boolean {
        val p = prefs(ctx)
        if (p.getString("key", null).isNullOrEmpty()) return false
        val expiry = p.getLong("expiry", 0L)
        return expiry == 0L || System.currentTimeMillis() < expiry
    }

    fun logout(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }

    // ── Modules ──
    fun setModuleEnabled(ctx: Context, moduleId: String, enabled: Boolean) {
        prefs(ctx).edit().putBoolean("mod_$moduleId", enabled).apply()
    }

    fun isModuleEnabled(ctx: Context, moduleId: String): Boolean =
        prefs(ctx).getBoolean("mod_$moduleId", false)

    /**
     * Phase-1 static roster. Phase 3 replaces with ModulesResponse from
     * the server; BinaryModule fields: id, name, version, abi, sha256,
     * downloadUrl, packageNames, enabled.
     */
    data class Module(
        val id: String,
        val name: String,
        val version: String,
        val description: String
    )

    fun defaultModules(): List<Module> = listOf(
        Module("core_overlay", "Soul Overlay", "1.0.0", "ImGui menu + ESP core (libhorcrux_payload.so)"),
        Module("ac_shield", "Anti-Cheat Veil", "1.0.0", "Managed + native detection voids"),
        Module("game_enhance", "Battle Enhancements", "1.0.0", "Aim assist, skins, auto features")
    )
}
