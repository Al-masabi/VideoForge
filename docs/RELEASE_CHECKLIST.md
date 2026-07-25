# قائمة تحقق الإصدار

## قبل الإصدار

- [ ] `./gradlew testDebugUnitTest` — كل الاختبارات خضراء
- [ ] `./gradlew assembleRelease` — البناء ينجح
- [ ] اختبار يدوي على جهاز حقيقي:
  - [ ] استيراد فيديو
  - [ ] قص + تراجع/إعادة
  - [ ] تصدير (COPY + TRANSCODE)
  - [ ] ضغط (Preset + CRF + Target Size)
  - [ ] معالجة دفعات
  - [ ] استيراد ترجمة + مزامنة
  - [ ] الإضافات (تفعيل/تعطيل/تحليل)
  - [ ] التشخيص (القراءات الحية)
- [ ] اختبار على جهاز Low-end (3GB RAM)
- [ ] اختبار RTL (العربية)
- [ ] اختبار Dark/Light theme
- [ ] اختبار High Contrast

## إعداد التوقيع

```bash
# إنشاء المفتاح
keytool -genkeypair -v \
  -keystore videoforge.keystore \
  -alias videoforge \
  -keyalg RSA -keysize 2048 -validity 10000

# تحويل إلى Base64
base64 -i videoforge.keystore > keystore_base64.txt
```

أضف الأسرار في GitHub: `Settings → Secrets → Actions`:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## الإطلاق

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions سيبني تلقائيًا:

- APK موقّع
- AAB موقّع (لـ Play Store)
- mapping.txt (للانهيارات)
- GitHub Release