# VideoForge — منصة معالجة الفيديو الاحترافية للأندرويد

```
┌──────────┐    ┌──────────┐    ┌───────────────┐    ┌─────────────┐    ┌──────────┐
│  Import  │ →  │  Probe   │ →  │  EDL Timeline │ →  │  Transform  │ →  │  Export  │
│ SAF      │    │ Metadata │    │ Split / Trim  │    │ MediaCodec  │    │ MP4      │
│ MediaStore│   │ Codecs   │    │ Delete / Undo │    │ Transformer │    │ Logs     │
└──────────┘    └──────────┘    └───────────────┘    └─────────────┘    └──────────┘
```

تطبيق أندرويد **Offline-first / Privacy-first** لتحرير الفيديو: قص بدقة الإطار، حذف مقاطع غير محدود، دمج، ضغط H.264/HEVC، وإدارة ترجمة كاملة — بدون إنترنت، بدون حسابات، بدون تتبع.

---

## الميزات

| المجال | التفاصيل |
|---|---|
| القص | Split / Trim / حذف متعدد / Undo-Redo / Timeline بصري / Scrub إطار-بإطار |
| المعاينة | ExoPlayer + Frame Stepping + A-B Repeat + سرعات 0.5x–2x |
| التصدير | COPY / LOSSLESS (FFmpeg) / TRANSCODE مع احترام كامل للـ EDL |
| الضغط | Presets + CRF حقيقي (libx264) + حجم مستهدف + تقدير حي |
| الترجمة | SRT/VTT + كشف ترميز + مزامنة تلقائية بعد القص |
| الدفعات | Task Queue + Foreground Service + إلغاء لكل مهمة |
| التكيف | تصنيف الجهاز + إيقاف حراري + حدود ذاكرة + كاش ذكي |
| الإضافات | منصة Plugin API مع إضافتين مدمجتين |

---

## البدء السريع

```bash
# 1) تنزيل الخطوط (إلزامي قبل البناء)
chmod +x scripts/setup-fonts.sh && ./scripts/setup-fonts.sh

# 2) البناء
./gradlew assembleDebug

# 3) الاختبارات
./gradlew testDebugUnitTest
```

---

## البناء عبر GitHub Actions

- **Debug APK**: يُبنى تلقائيًا عند كل `push` → `Actions → Build Debug APK → Artifacts`
- **Release موقّع**: أضف أسرار التوقيع ثم `git tag v1.0.0 && git push origin v1.0.0`
- **FFmpeg Native**: `Actions → Build FFmpeg Native → Run workflow` (اختياري)

---

## المعمارية

```
:app
├── :core:designsystem   (ثيم Projector + مكونات + حركة + أيقونات)
├── :core:data ──┬── :core:database   (Room — 9 جداول)
│                ├── :core:datastore  (Preferences)
│                ├── :core:media      (Metadata + Waveform)
│                └── :core:subtitle   (SRT/VTT + مزامنة)
├── :core:adaptive       (Device Profiling + سياسات)
├── :plugin:api          (عقود الإضافات)
├── :engine:ffmpeg-native (JNI + libav + libx264)
└── :benchmark           (Baseline Profiles)
```

التفاصيل: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## الخصوصية

- لا إذن `INTERNET` إطلاقًا
- لا إعلانات، لا تتبع، لا حسابات
- كل المعالجة محلية على الجهاز

## الترخيص

MIT — انظر [LICENSE](LICENSE)