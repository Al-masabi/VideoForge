# دليل المطور

## المتطلبات

- JDK 17+
- Android SDK 36
- Gradle 8.13+
- NDK + CMake (اختياري — لبناء FFmpeg)

## البناء

```bash
# بناء Debug APK
./gradlew assembleDebug

# تشغيل الاختبارات
./gradlew testDebugUnitTest

# بناء FFmpeg (اختياري — يتطلب NDK)
./scripts/setup-ffmpeg.sh
./gradlew assembleDebug
```

## إضافة شاشة جديدة

1. أنشئ `app/src/main/kotlin/com/videoforge/android/ui/<name>/<Name>Screen.kt`
2. أنشئ `<Name>ViewModel.kt` مع `@HiltViewModel`
3. أضف المسار في `AppRoutes` و`AppNavHost`
4. أضف النصوص في `strings.xml`

## إضافة Preset ضغط جديد

1. أضف عنصرًا في `CompressionPresets.ALL` في `CompressionModels.kt`
2. أضف النصوص في `strings.xml`
3. لا شيء آخر — الـ UI يقرأ القائمة ديناميكيًا

## إضافة إضافة (Plugin) جديدة

انظر [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md)

## الاختبارات

```bash
./gradlew testDebugUnitTest                          # اختبارات الوحدة
./gradlew :core:database:connectedDebugAndroidTest   # اختبارات Room
./gradlew :benchmark:connectedCheck                  # القياسات
```