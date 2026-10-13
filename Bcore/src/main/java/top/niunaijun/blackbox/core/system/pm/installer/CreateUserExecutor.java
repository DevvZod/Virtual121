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
            String[] subdirs = new String[]{
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
                    "comlibs",
                    "comlibs/arm64-v8a",
                    "comlibs/armeabi-v7a",
                    "UI",
                    "UI/android",
                    "Art",
                    "Art/android",
                    "AStarPath"
            };

            File externalFiles = BEnvironment.getExternalDataFilesDir(packageName, userId);
            File internalFiles = BEnvironment.getDataFilesDir(packageName, userId);
            File[] bases = new File[]{
                    externalFiles,
                    internalFiles,
                    new File(externalFiles, "dragon2017"),
                    new File(internalFiles, "dragon2017"),
                    new File(externalFiles, "dragon2017/assets"),
                    new File(internalFiles, "dragon2017/assets")
            };

            for (File base : bases) {
                for (String sub : subdirs) {
                    FileUtils.mkdirs(new File(base, sub));
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
