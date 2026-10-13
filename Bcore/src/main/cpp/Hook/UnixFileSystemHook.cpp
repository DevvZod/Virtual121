//
// Created by Milk on 4/9/21.
//

#include <IO.h>
#include "UnixFileSystemHook.h"
#import "JniHook/JniHook.h"
#include "BaseHook.h"

/*
 * Class:     java_io_UnixFileSystem
 * Method:    canonicalize0
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
HOOK_JNI(jstring, canonicalize0, JNIEnv *env, jobject obj, jstring path) {
    jstring redirect = IO::redirectPath(env, path);
    return orig_canonicalize0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    canonicalize0 (Android 16+)
 * Signature: (Ljava/lang/String;Z)Ljava/lang/String;
 */
HOOK_JNI(jstring, canonicalize0_v2, JNIEnv *env, jobject obj, jstring path, jboolean isAtLeastTargetSdk35) {
    jstring redirect = IO::redirectPath(env, path);
    return orig_canonicalize0_v2(env, obj, redirect, isAtLeastTargetSdk35);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    getBooleanAttributes0
 * Signature: (Ljava/lang/String;)I
 */
HOOK_JNI(jint, getBooleanAttributes0, JNIEnv *env, jobject obj, jstring abspath) {
    jstring redirect = IO::redirectPath(env, abspath);
    return orig_getBooleanAttributes0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    getLastModifiedTime0
 * Signature: (Ljava/io/File;)J
 */
HOOK_JNI(jlong, getLastModifiedTime0, JNIEnv *env, jobject obj, jobject path) {
    jobject redirect = IO::redirectPath(env, path);
    return orig_getLastModifiedTime0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    setPermission0
 * Signature: (Ljava/io/File;IZZ)Z
 */
HOOK_JNI(jboolean, setPermission0, JNIEnv *env, jobject obj, jobject file, jint access,
         jboolean enable, jboolean owneronly) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_setPermission0(env, obj, redirect, access, enable, owneronly);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    createFileExclusively0
 * Signature: (Ljava/lang/String;)Z
 */
HOOK_JNI(jboolean, createFileExclusively0, JNIEnv *env, jobject obj, jstring path) {
    jstring redirect = IO::redirectPath(env, path);
    return orig_createFileExclusively0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    list0
 * Signature: (Ljava/io/File;)[Ljava/lang/String;
 */
HOOK_JNI(jobjectArray, list0, JNIEnv *env, jobject obj, jobject file) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_list0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    createDirectory0
 * Signature: (Ljava/io/File;)Z
 */
HOOK_JNI(jboolean, createDirectory0, JNIEnv *env, jobject obj, jobject path) {
    jobject redirect = IO::redirectPath(env, path);
    return orig_createDirectory0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    setLastModifiedTime0
 * Signature: (Ljava/io/File;J)Z
 */
HOOK_JNI(jboolean, setLastModifiedTime0, JNIEnv *env, jobject obj, jobject file, jobject time) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_setLastModifiedTime0(env, obj, redirect, time);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    setReadOnly0
 * Signature: (Ljava/io/File;)Z
 */
HOOK_JNI(jboolean, setReadOnly0, JNIEnv *env, jobject obj, jobject file) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_setReadOnly0(env, obj, redirect);
}

/*
 * Class:     java_io_UnixFileSystem
 * Method:    getSpace0
 * Signature: (Ljava/io/File;I)J
 */
// FIX: getSpace0 returns a long (bytes). It was declared jboolean, which
// truncated the result to its low 8 bits (0-255 bytes) -> the game's
// free-space guard always saw "not enough space".
HOOK_JNI(jlong, getSpace0, JNIEnv *env, jobject obj, jobject file, jint t) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_getSpace0(env, obj, redirect, t);
}
/*
 * Class:     java_io_UnixFileSystem
 * Method:    checkAccess
 * Signature: (Ljava/io/File;I)Z
 */
HOOK_JNI(jboolean, checkAccess, JNIEnv *env, jobject obj, jobject file, jint access) {
    jobject redirect = IO::redirectPath(env, file);
    return orig_checkAccess(env, obj, redirect, access);
}

void UnixFileSystemHook::init(JNIEnv *env) {
    const char *className = "java/io/UnixFileSystem";
    // Android 16+ canonicalize0(String, boolean) — try the new signature
    // first, then the legacy one-arg form. The non-matching attempt logs
    // "jni hook unavailable" and skips gracefully (hardened HookJniFun).
    JniHook::HookJniFun(env, className, "canonicalize0", "(Ljava/lang/String;Z)Ljava/lang/String;",
                        (void *) new_canonicalize0_v2, (void **) (&orig_canonicalize0_v2), false);
    JniHook::HookJniFun(env, className, "canonicalize0", "(Ljava/lang/String;)Ljava/lang/String;",
                        (void *) new_canonicalize0, (void **) (&orig_canonicalize0), false);
    // RE-ENABLED on Android 16: File.isDirectory()/isFile()/exists()-style
    // checks all route through getBooleanAttributes0(f.getPath()). Without
    // this hook, every redirect-based directory check ran against the REAL
    // path — which is another app's Android/data, blocked by scoped
    // storage — so the guest's resource extraction aborted with
    // "prepareOutputDir, is not directory" and the game showed
    // "Extract Failed". The method is still native on Android 16 per
    // libcore main branch.
    JniHook::HookJniFun(env, className, "getBooleanAttributes0", "(Ljava/lang/String;)I",
                        (void *) new_getBooleanAttributes0,
                        (void **) (&orig_getBooleanAttributes0), false);
    JniHook::HookJniFun(env, className, "getLastModifiedTime0", "(Ljava/io/File;)J",
                        (void *) new_getLastModifiedTime0, (void **) (&orig_getLastModifiedTime0),
                        false);
    JniHook::HookJniFun(env, className, "setPermission0", "(Ljava/io/File;IZZ)Z",
                        (void *) new_setPermission0, (void **) (&orig_setPermission0), false);
    JniHook::HookJniFun(env, className, "createFileExclusively0", "(Ljava/lang/String;)Z",
                        (void *) new_createFileExclusively0,
                        (void **) (&orig_createFileExclusively0), false);
    JniHook::HookJniFun(env, className, "list0", "(Ljava/io/File;)[Ljava/lang/String;",
                        (void *) new_list0, (void **) (&orig_list0), false);
    JniHook::HookJniFun(env, className, "createDirectory0", "(Ljava/io/File;)Z",
                        (void *) new_createDirectory0, (void **) (&orig_createDirectory0), false);
    JniHook::HookJniFun(env, className, "setLastModifiedTime0", "(Ljava/io/File;J)Z",
                        (void *) new_setLastModifiedTime0, (void **) (&orig_setLastModifiedTime0),
                        false);
    JniHook::HookJniFun(env, className, "setReadOnly0", "(Ljava/io/File;)Z",
                        (void *) new_setReadOnly0, (void **) (&orig_setReadOnly0), false);
    JniHook::HookJniFun(env, className, "getSpace0", "(Ljava/io/File;I)J",
                        (void *) new_getSpace0, (void **) (&orig_getSpace0), false);
    JniHook::HookJniFun(env, className, "checkAccess", "(Ljava/io/File;I)Z",
                        (void *) new_checkAccess, (void **) (&orig_checkAccess), false);
    JniHook::HookJniFun(env, className, "checkAccess0", "(Ljava/io/File;I)Z",
                        (void *) new_checkAccess, (void **) (&orig_checkAccess), false);
}