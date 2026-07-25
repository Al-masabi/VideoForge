# سجل التغييرات

## 1.0.0 — 2026-07-25

### الإصدار الأول الكامل

- استيراد الفيديو عبر SAF وMediaStore مع Metadata كاملة
- محرر EDL غير تدميري: Split / Trim / Delete / Markers
- Undo/Redo عبر Snapshots موثوقة
- مشغل ExoPlayer: Frame Stepping، A-B Repeat، سرعات 0.5x–2x
- Timeline بصري: مسطرة زمنية، شريط فيلم، Waveform، تقريب/إبعاد
- Scrub إطار-بإطار مع معاينة Surface مخصصة
- تصدير Lossless Copy أو Transcode حسب حالة التحرير
- محرك ترجمة SRT/VTT مع كشف ترميز ومزامنة تلقائية بعد القص
- ضغط H.264/HEVC بأربعة Presets + CRF حقيقي + حجم مستهدف
- معالجة دفعات عبر Task Queue وForeground Service
- محرك تكيفي: تصنيف الأجهزة، إيقاف حراري، حدود ذاكرة
- منصة إضافات مع إضافتين مدمجتين
- لوحة تشخيص حية للمطورين
- هوية بصرية "Projector": Cairo + IBM Plex + حركة موحدة
- 30+ اختبار وحدة + Integration Tests + Macrobenchmarks
- توقيع Release تلقائي عبر GitHub Actions (APK + AAB)