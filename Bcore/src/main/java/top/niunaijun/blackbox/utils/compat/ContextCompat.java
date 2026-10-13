package top.niunaijun.blackbox.utils.compat;

import android.content.Context;
import android.content.ContextWrapper;

import black.android.app.BRContextImpl;
import black.android.app.BRContextImplKitkat;
import black.android.content.AttributionSourceStateContext;
import black.android.content.BRAttributionSource;
import black.android.content.BRAttributionSourceState;
import black.android.content.BRContentResolver;
import java.io.File;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.FileUtils;

/**
 * Created by Milk on 3/31/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class ContextCompat {
    public static final String TAG = "ContextCompat";

    public static void fixAttributionSourceState(Object obj, int uid) {
        Object mAttributionSourceState;
        if (obj != null && BRAttributionSource.get(obj)._check_mAttributionSourceState() != null) {
            mAttributionSourceState = BRAttributionSource.get(obj).mAttributionSourceState();

            AttributionSourceStateContext attributionSourceStateContext = BRAttributionSourceState.get(mAttributionSourceState);
            attributionSourceStateContext._set_packageName(BlackBoxCore.getHostPkg());
            attributionSourceStateContext._set_uid(uid);
            fixAttributionSourceState(BRAttributionSource.get(obj).getNext(), uid);
        }
    }

    public static void fix(Context context) {
        try {
            int deep = 0;
            while (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
                deep++;
                if (deep >= 10) {
                    return;
                }
            }
            BRContextImpl.get(context)._set_mPackageManager(null);
            try {
                context.getPackageManager();
            } catch (Throwable e) {
                e.printStackTrace();
            }

            BRContextImpl.get(context)._set_mBasePackageName(BlackBoxCore.getHostPkg());
            BRContextImplKitkat.get(context)._set_mOpPackageName(BlackBoxCore.getHostPkg());
            BRContentResolver.get(context.getContentResolver())._set_mPackageName(BlackBoxCore.getHostPkg());

            String packageName = BActivityThread.getAppPackageName();
            if (packageName == null) {
                try {
                    packageName = context.getPackageName();
                } catch (Throwable ignored) {
                }
            }
            int userId = BActivityThread.getUserId();
            if (packageName != null) {
                File externalFilesDir = BEnvironment.getExternalDataFilesDir(packageName, userId);
                File externalCacheDir = BEnvironment.getExternalDataCacheDir(packageName, userId);
                File externalMediaDir = new File(BEnvironment.getExternalDataDir(packageName, userId), "media");
                FileUtils.mkdirs(externalFilesDir);
                FileUtils.mkdirs(externalCacheDir);
                FileUtils.mkdirs(externalMediaDir);
                setField(context, "mExternalFilesDirs", new File[]{externalFilesDir});
                setField(context, "mExternalCacheDirs", new File[]{externalCacheDir});
                setField(context, "mExternalMediaDirs", new File[]{externalMediaDir});
            }

            if (BuildCompat.isS()) {
                fixAttributionSourceState(BRContextImpl.get(context).getAttributionSource(), BActivityThread.getBUid());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void setField(Object target, String fieldName, Object val) {
        if (target == null) return;
        for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, val);
                return;
            } catch (Throwable ignored) {
            }
        }
    }
}
