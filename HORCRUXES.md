# HORCRUXES — Dark Arts Container

> BlackBox-based virtual engine (Apache-2.0), modernized for Android 16
> (compileSdk 36 / targetSdk 36), HORCRUXES module system.
> Black + Avada-green, Dark Wizard theme.

```
HOST: com.horcruxes.box (horcruxes/)
  LoginActivity   → key gate (Phase 3: server auth)
  MainActivity    → install/bind game (SAF picker), launch via BlackBoxCore
  ModulesActivity → module toggles (Phase 3: BinaryModule store)
  HorcruxLoader   → BlackBoxCore.doAttachBaseContext + doCreate +
                    addAppLifecycleCallback
  ModuleInjector  → runs INSIDE GUEST at beforeApplicationOnCreate:
                    match Unity guests ("UnityKillsMe"/":unity") →
                    session dedup → System.load(payload .so) → fail-open
  PayloadDeployer → assets/horcrux/libhorcrux_payload.so → guest filesDir
```

## Build status: ✅ WORKING

```
> gradlew :horcruxes:assembleDebug
BUILD SUCCESSFUL
→ horcruxes/build/outputs/apk/debug/horcruxes-debug.apk (~7MB)
```

## Toolchain (modernized from upstream AGP 4.0.1)

| Component | Version |
|---|---|
| AGP | 8.9.1 |
| Gradle | 8.11.1 (wrapper updated) |
| Kotlin | 1.9.24 |
| JDK | **21** (Temurin, at `Apktool_M/jdk-21.0.12.1+1`, set via `org.gradle.java.home` in gradle.properties — JDK 25 NOT supported by Gradle 8.x) |
| compileSdk / targetSdk | 36 (Android 16) / 36 |
| build-tools | 35.0.0 (AGP 8.9 minimum; 30.0.3 ignored) |
| jcenter | removed → google() + mavenCentral() + jitpack |
| free_reflection | `me.weishu:free_reflection:3.0.1` (jcenter, dead) → `com.github.tiann:FreeReflection:3.2.2` (jitpack) |

## Upstream fixes applied during migration

1. **Namespaces** — every module got `namespace` in build.gradle; `package=`
   stripped from all 9 AndroidManifests (AGP 8 requirement).
2. **AIDL enablement** — `buildFeatures { aidl true }` in Bcore +
   android-mirror (AGP 8 defaults it off; mirror's IServiceConnection /
   IAccountManagerResponse broke Bcore aidl imports without it).
3. **AIDL export** — `aidlPackagedList` in android-mirror (AGP 8 replacement
   of the old `aidlPackageWhiteList`) so Bcore can import the mirror's
   framework interfaces.
4. **Windows AIDL unicode bug** — build-tools 33+ aidl.exe embeds the full
   command line in generated headers; project paths containing `\user\`
   (…core\system\user\…) become illegal `\u` escapes for javac even inside
   comments. Fixed with a `stripAidlHeaders` Gradle task (Bcore +
   android-mirror) that rewrites the generated headers after AIDL,
   before javac.
5. **BLocationConfig.aidl** — package statement corrected to
   `top.niunaijun.blackbox.entity.location` (matching file path + java class;
   strict aidl.exe check).
6. **pine-core** — NDK 27 (AGP 8.9 default): missing `#include <cstddef>`
   for `std::nullptr_t` (scoped_local_ref.h); ARM32 trampoline assembly
   fails lld PIC checks → pine abiFilters = arm64-v8a only (the game is
   arm64; 32-bit guests unsupported — acceptable).
7. **jcenter deps** — all artifacts re-resolved from mavenCentral/jitpack.

## Run requirements (Phase 1 demo)

- Install the APK, grant "unknown sources".
- LOGIN: any key ≥4 chars (Phase 3 wires the real key server).
- INSTALL GAME: pick the MLBB APK — BlackBox installs it into the
  container (no root).
- LAUNCH: the game boots inside the virtual container.
- `adb logcat -s HORCRUX HORCRUX-INJ HORCRUX-DEPLOY` shows the lifecycle:
  beforeApplicationOnCreate fires per guest process; Unity guests get the
  payload deploy attempt ("missing payload .so" until Phase 4 ships the
  actual `libhorcrux_payload.so` asset — fail-open by design).

## Phase checklist

- [x] Phase 1 — fork + modern toolchain + HORCRUXES host app (BUILD OK)
- [x] Phase 2a — ModuleInjector (beforeApplicationOnCreate seam, Unity
      process matching, session dedup, fail-open)
- [x] Phase 2b — PayloadDeployer (assets → guest dir)
- [ ] Phase 2c — native bootstrap (linker-namespace hardening, NeverLose
      libnlb equivalent) if needed after Phase 4 testing
- [ ] Phase 3 — Horcrux server glue: Login/AuthSession, Modules store
      (BinaryModule: downloadUrl + sha256 + kill-switch)
- [ ] Phase 4 — payload pipeline: current mod libc++_shared.so →
      assets/horcrux/libhorcrux_payload.so (strip loader-era assumptions)
- [ ] Phase 5 — device matrix: OPPO CPH1911 MTK Android 11 first — the
      destroyed-mutex crash class should be dead by architecture
      (deterministic spawn-time injection, no remote ptrace race)

## Payload contract (native mod)

The guest injector System.load()s `files/horcrux/libhorcrux_payload.so`.
Its JNI_OnLoad keeps current behavior (shadowhook init, eglSwapBuffers
hook, ImGui, input hooks) minus external-injector assumptions.

## Legal
Engine: Apache-2.0 (upstream BlackBox, LICENSE at repo root). Keep the
upstream NOTICE/attribution in any distribution.
