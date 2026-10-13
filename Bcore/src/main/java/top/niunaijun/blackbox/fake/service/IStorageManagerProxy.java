package top.niunaijun.blackbox.fake.service;

import android.os.IInterface;
import android.os.storage.StorageVolume;

import java.io.File;
import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import black.android.os.mount.BRIMountServiceStub;
import black.android.os.storage.BRIStorageManagerStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.core.IOCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.compat.BuildCompat;

/**
 * Created by Milk on 4/10/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 此处无Bug
 */
public class IStorageManagerProxy extends BinderInvocationStub {

    public IStorageManagerProxy() {
        super(BRServiceManager.get().getService("mount"));
    }

    @Override
    protected Object getWho() {
        IInterface mount;
        if (BuildCompat.isOreo()) {
            mount = BRIStorageManagerStub.get().asInterface(BRServiceManager.get().getService("mount"));
        } else {
            mount = BRIMountServiceStub.get().asInterface(BRServiceManager.get().getService("mount"));
        }
        return mount;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("mount");
        replaceSystemService("storage");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getVolumeList")
    public static class GetVolumeList extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object result = null;
            try {
                result = method.invoke(who, args);
            } catch (Throwable ignored) {
            }
            if (result instanceof StorageVolume[]) {
                StorageVolume[] volumes = (StorageVolume[]) result;
                File targetDir = BEnvironment.getExternalUserDir(BActivityThread.getUserId());
                FileUtils.mkdirs(targetDir);
                for (StorageVolume volume : volumes) {
                    patchVolume(volume, targetDir);
                }
                return volumes;
            }
            try {
                StorageVolume[] volumeList = BlackBoxCore.getBStorageManager().getVolumeList(BActivityThread.getBUid(), null, 0, BActivityThread.getUserId());
                if (volumeList != null) {
                    return volumeList;
                }
            } catch (Throwable ignored) {
            }
            return result;
        }
    }

    @ProxyMethod("getVolumes")
    public static class GetVolumes extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object result = method.invoke(who, args);
            if (result != null && result.getClass().isArray()) {
                File targetDir = BEnvironment.getExternalUserDir(BActivityThread.getUserId());
                FileUtils.mkdirs(targetDir);
                int len = java.lang.reflect.Array.getLength(result);
                for (int i = 0; i < len; i++) {
                    Object item = java.lang.reflect.Array.get(result, i);
                    patchVolume(item, targetDir);
                }
            }
            return result;
        }
    }

    private static void patchVolume(Object volume, File targetDir) {
        if (volume == null || targetDir == null) return;
        for (Class<?> c = volume.getClass(); c != null; c = c.getSuperclass()) {
            for (String fieldName : new String[]{"mPath", "mInternalPath", "mActivitySecurePath", "path", "internalPath"}) {
                try {
                    java.lang.reflect.Field f = c.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    if (f.getType() == File.class) {
                        f.set(volume, targetDir);
                    } else if (f.getType() == String.class) {
                        f.set(volume, targetDir.getAbsolutePath());
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @ProxyMethod("mkdirs")
    public static class mkdirs extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof String) {
                        String str = (String) arg;
                        if (str.startsWith("/")) {
                            File f = new File(IOCore.get().redirectPath(str));
                            FileUtils.mkdirs(f);
                        }
                    }
                }
            }
            return 0;
        }
    }
}
