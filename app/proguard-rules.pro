# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in $ANDROID_HOME/tools/proguard/proguard-android-optimize.txt

# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.rahmanilab.lexico.**$$serializer { *; }
-keepclasseswithmembers class com.rahmanilab.lexico.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rahmanilab.lexico.**$$serializer { *; }

# Room generated code.
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
