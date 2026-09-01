# ساخت APK در ویندوز

پیش‌نیازهای این نسخه:

- JDK 21
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0
- CMake 3.22.1
- NDK (Side by side)، نسخه پیشنهادی Android Studio

در Android Studio مقدار `Gradle JDK` را روی JBR 21 قرار دهید. سپس از ترمینال ریشه پروژه اجرا کنید:

```powershell
$env:JAVA_HOME="C:\Users\h.mohamadi\.jdks\jbr-21.0.11"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat --stop
.\gradlew.bat assembleDebug --refresh-dependencies
```

خروجی قابل نصب در مسیر زیر ساخته می‌شود:

```text
app\build\outputs\apk\debug\app-debug.apk
```

اگر Google Maven از شبکه شرکت `404` برگرداند، Build را با اینترنت مستقیم، هات‌اسپات یا VPN انجام دهید و پراکسی اجباری را از `gradle.properties` حذف کنید.
