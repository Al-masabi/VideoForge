# FFmpeg في VideoForge

## الحالة

FFmpeg **اختياري**. التطبيق يعمل بدونه (عبر MediaCodec/Transformer).

عند إضافته، يُفعَّل:

- القص بلا خسارة (Lossless Cut) عند Keyframes
- ترميز CRF حقيقي (x264)

## البناء

```bash
# يتطلب NDK + CMake
./scripts/setup-ffmpeg.sh
./gradlew assembleDebug
```

السكربت يبني FFmpeg لـ:

- arm64-v8a
- armeabi-v7a

## الترخيص

⚠️ FFmpeg مع libx264 يخضع لرخصة **GPL**.

إذا وزّعت التطبيق مع هذه المكتبات، يجب الالتزام بـ GPL (نشر المصدر).

للتوزيع بدون GPL:

- لا تبنِ FFmpeg (التطبيق يعمل بدونه)
- أو ابنِ FFmpeg بدون libx264 (LGPL)