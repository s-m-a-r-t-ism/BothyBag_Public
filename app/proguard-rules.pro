# Keep Gson serialized classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.bothybag.app.data.Bothy { *; }
-keep class com.google.gson.** { *; }
