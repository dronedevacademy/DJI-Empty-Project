# DJI MSDK V5 — Android Integration Guide

A step-by-step guide to integrating the **DJI Mobile SDK V5 (MSDK V5)** into a fresh Android project.  
This repository contains a minimal working Android app that has the SDK wired up and ready to use as a starting point.

---

# Suggested Versions (DJI)

| Tool / Setting | Version |
|---|---|
| Android Studio | `Android Studio Narwhal 3 Feature Drop \| 2025.1.3` |
| Java Runtime | `17` |
| Kotlin | `2.1.0` |
| Gradle | `8.12` |
| Android Gradle Plugin | `8.7.0` |
| minSdkVersion | `24` |
| targetSdkVersion | `35` |

> These are the versions officially recommended by DJI for MSDK V5. Using newer versions — especially AGP or Gradle — may cause build failures.

---

## Prerequisites

- Android Studio (Android Studio Narwhal 3 Feature Drop \| 2025.1.3)
- A DJI Developer account → [developer.dji.com](https://developer.dji.com/)
- A DJI app key (generated from the developer portal)
- Java 17 (set as the Gradle JDK)
- A DJI supported controller

---

## Table of Contents

1. [Gradle & Toolchain Setup](#1-gradle--toolchain-setup)
2. [Build Configuration](#2-build-configuration)
3. [Packaging & Native Libraries](#4-packaging--native-libraries)
4. [Dependencies](#3-dependencies)
5. [UX SDK Module](#5-ux-sdk-module)
6. [AndroidManifest Configuration](#6-androidmanifest-configuration)
7. [Application Class](#7-application-class)
8. [First Run Checklist](#first-run-checklist)

---

# 1. Gradle & Toolchain Setup

DJI MSDK V5 requires specific versions of the Gradle wrapper, AGP, Kotlin, and Java. Using newer versions than those listed below may cause build failures.

### 1.1 Gradle Wrapper → 8.12

In `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.12-bin.zip
```
You can also change this from **File → Project Structure → Gradle Version**.

### 1.2 Android Gradle Plugin (AGP) → 8.7.0

In `gradle/libs.versions.toml`, under `[versions]`:
```toml
agp = "8.7.0"
```

### 1.3 Kotlin Plugin → 2.1.0

In `gradle/libs.versions.toml`, under `[versions]`:
```toml
kotlin = "2.1.0"
```

### 1.4 Java Runtime → 17

DJI explicitly recommends Java 17.  
Go to **Settings → Build, Execution, Deployment → Build Tools → Gradle** and set:
```
Gradle JDK = 17   (or "Embedded JDK" if it is already version 17)
```

---

# 2. Build Configuration

### 2.1 compileSdk / targetSdk / minSdk

In `app/build.gradle`:
```groovy
android {
    compileSdk 35

    defaultConfig {
        applicationId "com.example.myapplication"
        minSdk     24
        targetSdk  35
        // ...
    }
}
```
> DJI recommends `compileSdk 35`. Using 36 may work but is untested with this SDK version.

### 2.2 Downgrade Core Dependencies

Newer versions of these libraries can conflict with the SDK.  
In `gradle/libs.versions.toml`:
```toml
coreKtx  = "1.15.0"
activity = "1.9.3"
```

### 2.3 Enable kotlin-kapt

The SDK uses annotation processors, so `kapt` must be enabled.  
In `app/build.gradle` plugins block:
```groovy
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")       // ← add this
}
```

> **Sync the project here and then continue **

### 2.4 Force ABI to arm64-v8a

DJI only ships arm64 native libs. Filtering to a single ABI avoids packaging issues and speeds up builds:
```groovy
android {
    defaultConfig {
        ndk {
            abiFilters += "arm64-v8a"
        }
    }
}
```
---

# 3. Packaging & Native Libraries

The DJI SDK bundles many `.so` files. Without the rules below you will get `duplicate entry` errors at build time and missing symbols at runtime.

In `app/build.gradle`, inside the `android` block:
```groovy
android {
    packaging {
        jniLibs {
            // Resolve the libc++ conflict that exists across multiple DJI AAR files
            pickFirsts.add("lib/arm64-v8a/libc++_shared.so")

            // Retain debug symbols for all DJI native libraries
            keepDebugSymbols += listOf(
                "**/libconstants.so",
                "**/libdji_innertools.so",
                "**/libdjibase.so",
                "**/libDJICSDKCommon.so",
                "**/libDJIFlySafeCore-CSDK.so",
                "**/libdjifs_jni-CSDK.so",
                "**/libDJIRegister.so",
                "**/libdjisdk_jni.so",
                "**/libDJIUpgradeCore.so",
                "**/libDJIUpgradeJNI.so",
                "**/libDJIWaypointV2Core-CSDK.so",
                "**/libdjiwpv2-CSDK.so",
                "**/libFlightRecordEngine.so",
                "**/libvideo-framing.so",
                "**/libwaes.so",
                "**/libagora-rtsa-sdk.so",
                "**/libc++.so",
                "**/libc++_shared.so",
                "**/libmrtc_28181.so",
                "**/libmrtc_agora.so",
                "**/libmrtc_core.so",
                "**/libmrtc_core_jni.so",
                "**/libmrtc_data.so",
                "**/libmrtc_log.so",
                "**/libmrtc_onvif.so",
                "**/libmrtc_rtmp.so",
                "**/libmrtc_rtsp.so"
            )
        }
    }
}
```

---

# 4. DJI SDK Dependencies

### 4.1 Add SDK Version to Version Catalog

In `gradle/libs.versions.toml`, under `[versions]`:
```toml
djiSdk = "5.17.0"
```

### 4.2 Declare Library Aliases

In `gradle/libs.versions.toml`, under `[libraries]`:
```toml
dji-sdk-v5-aircraft = { group = "com.dji", name = "dji-sdk-v5-aircraft", version.ref = "djiSdk" }
dji-sdk-v5-aircraft-provided = { group = "com.dji", name = "dji-sdk-v5-aircraft-provided", version.ref = "djiSdk" }
```

**What these two artifacts are:**
- `dji-sdk-v5-aircraft` — the actual runtime SDK AAR you ship with your app.
- `dji-sdk-v5-aircraft-provided` — compile-time stubs only (equivalent to `compileOnly`).

### 4.3 Use Them in app/build.gradle

```groovy
dependencies {

    implementation(libs.dji.sdk.v5.aircraft)                  // runtime SDK
    compileOnly(libs.dji.sdk.v5.aircraft.provided)            // compile-time stubs only

    // your other app dependencies...
}
```
> **Sync the project here and then continue **

---

# 5. UX SDK Module

The line `implementation project(":android-sdk-v5-uxsdk")` is **not** a Maven dependency — it references a local Gradle module inside your project. You must import it manually.

### Step 1 — Get the UX SDK source

Clone the official DJI SDK sample project if you do not have the module folder:
```
https://github.com/dji-sdk/Mobile-SDK-Android-V5
```
Copy the `android-sdk-v5-uxsdk` folder into the root of your project.

### Step 2 — Register the module in settings.gradle

```groovy
include(":android-sdk-v5-uxsdk")
```

### Step 3 — Add the module dependency

In `app/build.gradle`:
```groovy
dependencies {
    implementation(project(":android-sdk-v5-uxsdk"))
    // ...
}
```

### Step 4 — gradle.properties

Add these properties so the UX SDK module picks up the right versions:
```properties
android.enableJetifier=true

ANDROID_COMPILE_SDK_VERSION=35
ANDROID_MIN_SDK_VERSION=24
ANDROID_TARGET_SDK_VERSION=35

KOTLIN_VERSION=2.1.0
SDK_VERSION=5.17.0
```

### Step 5 — Create dependencies.gradle

Create a file named `dependencies.gradle` in the same directory as your root `build.gradle` and paste in the contents from the DJI sample project's `dependencies.gradle`.

```
https://github.com/dji-sdk/Mobile-SDK-Android-V5/blob/dev-sdk-main/SampleCode-V5/android-sdk-v5-as/dependencies.gradle
```

### Step 6 — Import dependencies.gradle from root build.gradle

At the very bottom of your root `build.gradle` (Project level):
```groovy
apply(from = rootProject.file("dependencies.gradle"))
```

> **Sync the project** after completing all UX SDK steps.

---

# 6. AndroidManifest Configuration

### 6.1 Basic Permissions

Add inside `<manifest>`:
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 6.2 USB Hardware Features

Required for connecting to the DJI remote controller via USB. Add inside `<manifest>`:
```xml
<uses-feature
    android:name="android.hardware.usb.host"
    android:required="false"/>
<uses-feature
    android:name="android.hardware.usb.accessory"
    android:required="true"/>
```

### 6.3 Application Tag

Add these attributes to the `<application>` tag:
```xml
<application
    android:name=".MyApplication"
    android:extractNativeLibs="true"
    ... >
```
> `android:extractNativeLibs="true"` is required because Android API 35 changed the default behavior for extracting native libraries, and the DJI SDK depends on the old behavior.

### 6.4 DJI App Key

Register on the [DJI Developer Portal](https://developer.dji.com) to obtain an app key, then add it inside the `<application>` tag:
```xml
<meta-data
    android:name="com.dji.sdk.API_KEY"
    android:value="YOUR_APP_KEY_HERE"/>
```

### 6.5 USB Accessory Intent Filter

Add inside your main `<activity>` tag so the app is launched when a DJI remote controller is connected via USB:
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <action android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>

<meta-data
    android:name="android.hardware.usb.action.USB_ACCESSORY_ATTACHED"
    android:resource="@xml/accessory_filter" />
```
Make sure `res/xml/accessory_filter.xml` exists (copy it from the DJI sample project).

---

# 7. Application Class

Create `MyApplication.kt` in your main source set:

```kotlin
import android.app.Application
import android.content.Context
import android.util.Log
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.ux.beta.core.util.Helper

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
    }

    // Called before onCreate — required by the DJI Helper
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()

        SDKManager.getInstance().init(this, object : SDKManagerCallback {

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                Log.i(TAG, "onInitProcess: $event")
                // Once initialization is complete, register with DJI servers
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onRegisterSuccess() {
                Log.i(TAG, "onRegisterSuccess: SDK registered successfully")
            }

            override fun onRegisterFailure(error: IDJIError?) {
                Log.i(TAG, "onRegisterFailure: $error")
            }

            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "onProductConnect: productId=$productId")
            }

            override fun onProductDisconnect(productId: Int) {
                Log.i(TAG, "onProductDisconnect: productId=$productId")
            }

            override fun onProductChanged(productId: Int) {
                Log.i(TAG, "onProductChanged: productId=$productId")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                Log.i(TAG, "onDatabaseDownloadProgress: ${current * 100 / total}%")
            }
        })
    }
}
```

**Why two lifecycle hooks?**
- `attachBaseContext` — DJI's `Helper.install()` must run before any DJI class is loaded, so it belongs here.
- `onCreate` — `SDKManager.init()` starts the SDK and triggers registration once initialization is complete.

---

# First Run Checklist

Before building and running on a device, verify the following:

- [ ] Gradle wrapper is `8.12`, AGP is `8.7.0`, Kotlin is `2.1.0`
- [ ] Gradle JDK is set to Java 17
- [ ] `compileSdk` and `targetSdk` are both `35`, `minSdk` is `24`
- [ ] `kapt` plugin is applied in `app/build.gradle`
- [ ] ABI filter is set to `arm64-v8a`
- [ ] `dji-sdk-v5-aircraft` is `implementation`, `dji-sdk-v5-aircraft-provided` is `compileOnly`
- [ ] `android-sdk-v5-uxsdk` module is copied, included in `settings.gradle`, and listed as a dependency
- [ ] `dependencies.gradle` exists and is applied at the bottom of the root `build.gradle`
- [ ] `gradle.properties` contains all five UX SDK properties
- [ ] All permissions and USB features are declared in `AndroidManifest.xml`
- [ ] `android:extractNativeLibs="true"` is on the `<application>` tag
- [ ] Your real DJI app key is in `<meta-data android:name="com.dji.sdk.API_KEY" .../>`
- [ ] `res/xml/accessory_filter.xml` exists
- [ ] `MyApplication.kt` is created and declared in the manifest as `android:name=".MyApplication"`
- [ ] Testing on a **physical arm64 device** (not an emulator)

---

# Troubleshooting

**Duplicate class / `libc++_shared.so` conflict**  
→ Make sure the `pickFirsts.add(...)` rule in `packaging.jniLibs` is present.

**`Helper.install()` crash on startup**  
→ Ensure `Helper.install(this)` is called in `attachBaseContext`, not `onCreate`.

**Registration fails (`onRegisterFailure`)**  
→ Check your app key, verify `INTERNET` permission, and confirm the package name matches the one registered on the DJI Developer Portal.

**Build error: "Unsupported class file major version"**  
→ Your Gradle JDK is not Java 17. Change it in **Settings → Build Tools → Gradle → Gradle JDK**.

**`Execution failed for task ':app:mergeNativeLibs'`**  
→ ABI filter is missing or `pickFirsts` is not configured. Review Sections 2.4 and 4.

---

## References

- [DJI MSDK V5 Documentation](https://developer.dji.com/doc/mobile-sdk-tutorial/en/)
- [DJI Mobile SDK Android V5 — GitHub](https://github.com/dji-sdk/Mobile-SDK-Android-V5)
- [DJI Developer Portal (App Key)](https://developer.dji.com/user/apps)
