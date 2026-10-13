package com.horcruxes.box

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * HORCRUXES — Dark Arts Container.
 *
 * A BlackBox-based virtual engine hosting the game in a soul-split
 * container, with a deterministic module injector that loads our native
 * payload BEFORE the guest's Application.onCreate.
 *
 * HiddenApiBypass: the engine's entire reflection layer (BlackReflection
 * accessors, path redirection, ART internals) depends on hidden APIs.
 * FreeReflection's old unseal stopped working on Android 13+, so every
 * accessor silently failed on Android 16 (the game fell back to the
 * host's external dir — "not enough space" symptom). Exempting ALL
 * hidden APIs here, at the very top of attachBaseContext, covers every
 * process of this app — host AND guest stubs — before ANY engine code
 * runs.
 */
class HorcruxesApp : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var mContext: Context

        @JvmStatic
        fun getContext(): Context = mContext
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        mContext = base!!
        Log.i("HORCRUX", "HORCRUXES v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE}) starting")
        try {
            HiddenApiBypass.addHiddenApiExemptions("")
            Log.i("HORCRUX", "hidden API exemptions applied (all)")
        } catch (t: Throwable) {
            Log.e("HORCRUX", "hidden API bypass failed", t)
        }
        HorcruxLoader.doAttachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        HorcruxLoader.doOnCreate(mContext)
    }
}
