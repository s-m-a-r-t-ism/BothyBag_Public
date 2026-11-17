# Rules for the release build, where R8 shrinks and renames code.

# Gson reads generic types and annotations through reflection
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Classes read or written as JSON. Their field names are the JSON keys, in
# bothies.json, in the cached news and in exported backups, so they must not
# be renamed or removed.
-keep class com.bothybag.app.data.Bothy { *; }
-keep class com.bothybag.app.data.NewsItem { *; }
-keep class com.bothybag.app.data.UserDataBackup$* { *; }

# The map library looks some of its classes up by name
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
