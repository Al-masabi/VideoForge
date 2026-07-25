# نظام التصميم — Projector

## الفلسفة

هوية "دار عرض": فحم دافئ، كهرماني الضوء، فسفور الـ Timecode.
ليست Material افتراضية — هوية مقصودة.

## الألوان

| الدور | Dark | Light |
|---|---|---|
| أساسي (كهرماني) | `#E8A33D` | `#7A4E00` |
| ثانوي (فسفوري) | `#63C9B8` | `#00695F` |
| ثالثي (وردي) | `#DE8E80` | `#9C4143` |
| أسطح | `#14110C → #342C21` | `#FBF8F1 → #E3DCCE` |

Dynamic Color متاح لكنه **معطل افتراضيًا** — الهوية أولًا.

## الخطوط

| الدور | الخط |
|---|---|
| Display / Headlines / Titles | Cairo (ExtraBold → SemiBold) |
| Body / Labels | IBM Plex Sans Arabic |
| Timecode / أرقام تقنية | IBM Plex Mono |

## الحركة

- `VfMotion.PressSpring` — كل عنصر قابل للضغط يتقلص 0.965
- `VfReveal(index)` — ظهور متتابع 45ms/عنصر
- Shared Element — صورة الفيديو → المشغل/المحرر
- Nav — fade + slide أفقي 240ms

## العمق

- حافة مضيئة 1px أعلى كل بطاقة (`luminousEdge`)
- توهج كهرماني محيطي أعلى Home
- 5 طبقات سطوح بلا ظلال ثقيلة