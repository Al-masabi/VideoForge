# معمارية VideoForge

## الطبقات

```
┌─────────────────────────────────────────────────┐
│  Presentation   Compose + ViewModel + UDF       │
├─────────────────────────────────────────────────┤
│  Domain         UseCases + Models + Rules       │
├─────────────────────────────────────────────────┤
│  Data           Repositories + Room + DataStore │
├─────────────────────────────────────────────────┤
│  Engine         MediaCodec / FFmpeg / Parsers   │
└─────────────────────────────────────────────────┘
```

## الوحدات (Modules)

```
:app                    → الشاشات + الخدمات + نقطة البداية
:core:designsystem      → الثيم + المكونات + الحركة + الأيقونات
:core:database          → Room (9 جداول + 5 هجرات)
:core:datastore         → DataStore Preferences
:core:data              → المستودعات (Repositories)
:core:media             → استخراج الوسائط + Waveform
:core:subtitle          → SRT/VTT + المزامنة
:core:adaptive          → Device Profiling + السياسات
:plugin:api             → عقود الإضافات
:engine:ffmpeg-native   → JNI + FFmpeg (اختياري)
:benchmark              → Baseline Profiles + Macrobenchmarks
```

## المبادئ

1. **تحرير غير تدميري**: الملفات الأصلية لا تُمس. كل التعديلات EDL.
2. **Undo/Redo عبر Snapshots**: كل تعديل يُحفظ كـ snapshot كامل.
3. **محرك تكيفي**: السياسات تتغير حسب الجهاز (Low/Mid/High).
4. **FFmpeg اختياري**: يعمل التطبيق بدونه (عبر MediaCodec/Transformer)، ويُضاف لاحقًا للقص بلا خسارة + CRF.
5. **Offline-first**: لا إنترنت، لا تتبع، لا حسابات.

## تدفق التصدير

```
EditorScreen → ExportService.start()
  → VideoExportEngine.export()
    → chooseStrategy():
       - WHOLE_COPY (ملف كامل بلا تعديل)
       - LOSSLESS_SEGMENTS (FFmpeg متاح + حدود عند Keyframes)
       - TRANSCODE (إعادة ترميز عبر Transformer)
  → OperationLogRepository.log()
  → Notification نتيجة
```