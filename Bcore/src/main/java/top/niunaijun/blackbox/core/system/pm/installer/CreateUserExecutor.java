package top.niunaijun.blackbox.core.system.pm.installer;

import java.io.File;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.pm.BPackageSettings;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.FileUtils;

/**
 * Created by Milk on 4/24/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 * 创建用户相关
 */
public class CreateUserExecutor implements Executor {

    @Override
    public int exec(BPackageSettings ps, InstallOption option, int userId) {
        String packageName = ps.pkg.packageName;
        FileUtils.deleteDir(BEnvironment.getDataLibDir(packageName, userId));

        ensureUserDirs(packageName, userId);
        return 0;
    }

    public static void ensureUserDirs(String packageName, int userId) {
        try {
            // create user dir (internal)
            FileUtils.mkdirs(BEnvironment.getDataDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataCacheDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataFilesDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDataDatabasesDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getDeDataDir(packageName, userId));

            // create user dir (external)
            FileUtils.mkdirs(BEnvironment.getExternalUserDir(userId));
            FileUtils.mkdirs(BEnvironment.getExternalDataDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getExternalDataFilesDir(packageName, userId));
            FileUtils.mkdirs(BEnvironment.getExternalDataCacheDir(packageName, userId));

            createGameAssetDirs(packageName, userId);
        } catch (Throwable ignored) {
        }
    }

    public static void createGameAssetDirs(String packageName, int userId) {
        try {
            // Asset subdirs that MLBB's 7-Zip extractor expects to exist before
            // it opens output files. ENOENT (error: 2) on any of these kills the
            // whole extraction task (failedCount++ → "no space" dialog).
            String[] assetSubdirs = new String[]{
                    "dragon2017",
                    "dragon2017/assets",
                    "dragon2017/assets/comlibs",
                    "dragon2017/assets/comlibs/arm64-v8a",
                    "dragon2017/assets/comlibs/armeabi-v7a",
                    "dragon2017/assets/UI",
                    "dragon2017/assets/UI/android",
                    "dragon2017/assets/Art",
                    "dragon2017/assets/Art/android",
                    "dragon2017/assets/AStarPath",
                    "dragon2017/assets/Audio",
                    "dragon2017/assets/Audio/android",
                    "dragon2017/assets/Data",
                    "dragon2017/assets/Data/android",
                    "dragon2017/assets/Movie",
                    "dragon2017/assets/Movie/android",
                    "dragon2017/assets/Scene",
                    "dragon2017/assets/Scene/android",
                    "dragon2017/assets/Config",
                    "assets",
                    "assets/comlibs",
                    "assets/comlibs/arm64-v8a",
                    "assets/comlibs/armeabi-v7a",
                    "assets/UI",
                    "assets/UI/android",
                    "assets/Art",
                    "assets/Art/android",
                    "assets/AStarPath",
                    // Flat paths -- also needed relative to files/ root
                    "comlibs",
                    "comlibs/arm64-v8a",
                    "comlibs/armeabi-v7a",
                    "UI",
                    "UI/android",
                    "Art",
                    "Art/android",
                    "AStarPath",
                    "Audio",
                    "Audio/android",
                    "Data",
                    "Data/android",
                    "Movie",
                    "Movie/android",
                    "Scene",
                    "Scene/android",
                    "Config",
            };

            // Subdirs needed INSIDE app_libs/ — libResources*.so archives extract
            // their payload (comlibs/arm64-v8a/libcsharp.bytes etc.) relative to
            // the app_libs/ directory. Without these the open() syscall gets ENOENT.
            String[] appLibsSubdirs = new String[]{
                    "comlibs",
                    "comlibs/arm64-v8a",
                    "comlibs/armeabi-v7a",
                    "UI",
                    "UI/android",
                    "Art",
                    "Art/android",
                    "AStarPath",
                    "Audio",
                    "Audio/android",
                    "Data",
                    "Data/android",
                    "Movie",
                    "Movie/android",
                    "Scene",
                    "Scene/android",
                    "Config",
            };

            // Subdirs needed at dataDir ROOT — Resources4-*.dat archives use relative
            // paths (UI/android/foo.unity3d, AStarPath/bar.xml, Art/android/baz.unity3d)
            // resolved against the game's working directory (dataDir root).
            String[] dataDirRootSubdirs = new String[]{
                    "comlibs",
                    "comlibs/arm64-v8a",
                    "comlibs/armeabi-v7a",
                    "UI",
                    "UI/android",
                    "Art",
                    "Art/android",
                    "AStarPath",
                    "Audio",
                    "Audio/android",
                    "Data",
                    "Data/android",
                    "Movie",
                    "Movie/android",
                    "Scene",
                    "Scene/android",
                    "Config",
                    "app_libs",
            };

            File externalFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
            File internalFiles = BEnvironment.getDataFilesDir(packageName, userId);
            File dataDir       = BEnvironment.getDataDir(packageName, userId);
            File appLibsDir    = new File(dataDir, "app_libs");

            // 1. Pre-create under files/ and external files/ (dragon2017/assets tree + flat)
            File[] filesBases = new File[]{
                    externalFiles,
                    internalFiles,
                    new File(externalFiles, "dragon2017"),
                    new File(internalFiles, "dragon2017"),
                    new File(externalFiles, "dragon2017/assets"),
                    new File(internalFiles, "dragon2017/assets"),
            };
            for (File base : filesBases) {
                for (String sub : assetSubdirs) {
                    FileUtils.mkdirs(new File(base, sub));
                }
            }

            // 2. Pre-create under app_libs/ (libResources*.so extraction root)
            FileUtils.mkdirs(appLibsDir);
            for (String sub : appLibsSubdirs) {
                FileUtils.mkdirs(new File(appLibsDir, sub));
            }

            // 3. Pre-create at dataDir root (Resources4-*.dat extraction root)
            for (String sub : dataDirRootSubdirs) {
                FileUtils.mkdirs(new File(dataDir, sub));
            }

        } catch (Throwable ignored) {
        }
    }
}
