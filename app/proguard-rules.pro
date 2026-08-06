# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mikhilnaika.continueapp.**$$serializer { *; }
-keepclassmembers class com.mikhilnaika.continueapp.** {
    *** Companion;
}
-keepclasseswithmembers class com.mikhilnaika.continueapp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
