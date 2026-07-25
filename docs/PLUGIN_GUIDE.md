# دليل الإضافات (Plugins)

## العقود المتاحة

```kotlin
interface VideoForgePlugin {
    val manifest: PluginManifest
}

interface SubtitlePlugin : VideoForgePlugin {
    fun transformCues(cues: List<PluginSubtitleCue>): List<PluginSubtitleCue>
}

interface AnalysisPlugin : VideoForgePlugin {
    suspend fun analyze(context: Context, uri: Uri): String
}
```

## إضافة Subtitle Plugin

```kotlin
class MySubtitlePlugin : SubtitlePlugin {
    override val manifest = PluginManifest(
        id = "my.plugin.id",
        name = "إضافتي",
        version = "1.0.0",
        description = "وصف الإضافية",
        type = PluginType.SUBTITLE
    )

    override fun transformCues(cues: List<PluginSubtitleCue>): List<PluginSubtitleCue> {
        return cues.map { it.copy(text = it.text.uppercase()) }
    }
}
```

## التسجيل

أضف الإضافية إلى قائمة `plugins` في `PluginRegistry.kt`:

```kotlin
private val plugins: List<VideoForgePlugin> = listOf(
    SubtitleShiftPlugin(),
    VideoInfoReportPlugin(),
    MySubtitlePlugin()  // ← هنا
)
```

## التفعيل/التعطيل

المستخدم يفعّل/يعطّل الإضافات من شاشة الإضافات. الحالة تُحفظ في DataStore.