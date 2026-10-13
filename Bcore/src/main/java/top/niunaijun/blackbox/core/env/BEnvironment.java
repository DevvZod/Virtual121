package top.niunaijun.blackbox.core.env;

import java.io.File;
import java.util.Locale;
import android.os.Environment;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.IOCore;
import top.niunaijun.blackbox.utils.FileUtils;

/**
 * Created by Milk on 4/22/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class BEnvironment {
    // HORCRUX storage model (ported from the modernized NeverLose fork):
    //  - Internal venv is NESTED under <app>/blackbox (isolated from the
    //    host app's own files/prefs).
    //  - External venv is the host's OWN app-specific external dir
    //    (getExternalFilesDir). That directory is ALWAYS writable on every
    //    Android version WITHOUT any storage permission (app-specific
    //    external storage since API 19), always exists (the system creates
    //    it), and statfs/getUsableSpace on it always reports the true free
    //    space of the volume.
    //
    //  The legacy /sdcard/AkatskiGamespace root (upstream) required
    //  MANAGE_EXTERNAL_STORAGE, failed mkdirs under scoped storage, and
    //  made statfs report 0 → the famous guest-side "not enough space"
    //  even with plenty of free storage. This port removes that class of
    //  failure entirely.
    private static final File sVirtualRoot =
            new File(BlackBoxCore.getContext().getCacheDir().getParent(), "blackbox");
    private static final File sExternalVirtualRoot = resolveExternalRoot();

    private static File resolveExternalRoot() {
        File dir = null;
        try {
            dir = BlackBoxCore.getContext().getExternalFilesDir("blackbox");
        } catch (Throwable ignored) {
        }
        if (dir == null) {
            // External storage unavailable — fall back to the internal venv
            // so the engine still boots (storage-dependent checks will
            // report the internal volume).
            dir = new File(BlackBoxCore.getContext().getCacheDir().getParent(), "blackbox_ext");
        }
        // Publish the resolved root to IOCore IMMEDIATELY. The OsStub
        // proxy is already installed while this class initializes, so any
        // File.exists() below this point re-enters IOCore.redirectPath —
        // which must never call back into this (mid-initialization) class.
        try {
            IOCore.cacheExternalRoot(dir.getAbsolutePath());
        } catch (Throwable ignored) {
        }
        return dir;
    }

    public static File JUNIT_JAR = new File(getCacheDir(), "junit.apk");
    public static File EMPTY_JAR = new File(getCacheDir(), "empty.apk");

    public static void load() {
        FileUtils.mkdirs(sVirtualRoot);
        FileUtils.mkdirs(sExternalVirtualRoot);
        FileUtils.mkdirs(getSystemDir());
        FileUtils.mkdirs(getCacheDir());
        FileUtils.mkdirs(getProcDir());
    }

    public static File getVirtualRoot() {
        return sVirtualRoot;
    }

    public static File getExternalVirtualRoot() {
        return sExternalVirtualRoot;
    }

    public static File getSystemDir() {
        return new File(sVirtualRoot, "system");
    }

    public static File getProcDir() {
        return new File(sVirtualRoot, "proc");
    }

    public static File getCacheDir() {
        return new File(sVirtualRoot, "cache");
    }

    public static File getUserInfoConf() {
        return new File(getSystemDir(), "user.conf");
    }

    public static File getAccountsConf() {
        return new File(getSystemDir(), "accounts.conf");
    }

    public static File getUidConf() {
        return new File(getSystemDir(), "uid.conf");
    }

    public static File getSharedUserConf() {
        return new File(getSystemDir(), "shared-user.conf");
    }

    public static File getXPModuleConf() {
        return new File(getSystemDir(), "xposed-module.conf");
    }

    public static File getFakeLocationConf() {
        return new File(getSystemDir(), "fake-location.conf");
    }

    public static File getPackageConf(String packageName) {
        return new File(getAppDir(packageName), "package.conf");
    }

    public static File getExternalUserDir(int userId) {
        return new File(sExternalVirtualRoot, String.format(Locale.CHINA, "%d", userId));
    }

    public static File getUserDir(int userId) {
        return new File(sVirtualRoot, String.format(Locale.CHINA, "data/user/%d", userId));
    }

    public static File getDeDataDir(String packageName, int userId) {
        return new File(sVirtualRoot, String.format(Locale.CHINA, "data/user_de/%d/%s", userId, packageName));
    }

    public static File getExternalDataDir(String packageName, int userId) {
        return new File(getExternalUserDir(userId), String.format(Locale.CHINA, "Android/data/%s", packageName));
    }


    public static File getDataDir(String packageName, int userId) {
        return new File(sVirtualRoot, String.format(Locale.CHINA, "data/user/%d/%s", userId, packageName));
    }

    public static File getProcDir(int pid) {
        File file = new File(getProcDir(), String.format(Locale.CHINA, "%d", pid));
        FileUtils.mkdirs(file);
        return file;
    }

    public static File getExternalDataFilesDir(String packageName, int userId) {
        return new File(getExternalDataDir(packageName, userId), "files");
    }

    public static File getDataFilesDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "files");
    }

    public static File getExternalDataCacheDir(String packageName, int userId) {
        return new File(getExternalDataDir(packageName, userId), "cache");
    }

    public static File getDataCacheDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "cache");
    }

    public static File getDataLibDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "lib");
    }

    public static File getDataDatabasesDir(String packageName, int userId) {
        return new File(getDataDir(packageName, userId), "databases");
    }

    public static File getAppRootDir() {
        return getAppDir("");
    }

    public static File getAppDir(String packageName) {
        return new File(sVirtualRoot, "data/app/" + packageName);
    }

    public static File getBaseApkDir(String packageName) {
        return new File(sVirtualRoot, "data/app/" + packageName + "/base.apk");
    }

    public static File getAppLibDir(String packageName) {
        return new File(getAppDir(packageName), "lib");
    }

    public static File getXSharedPreferences(String packageName, String prefFileName) {
       return new File(BEnvironment.getDataDir(packageName, BActivityThread.getUserId()), "shared_prefs/" + prefFileName + ".xml");
    }
}
