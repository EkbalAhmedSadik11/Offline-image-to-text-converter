# Add project specific ProGuard rules here.

# Tesseract4Android / JNI native bridge classes must keep their member
# names and signatures so the native (C++) side can find them via JNI.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class com.googlecode.leptonica.android.** { *; }
-keepclassmembers class com.googlecode.tesseract.android.** { *; }
-keepclassmembers class com.googlecode.leptonica.android.** { *; }

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
