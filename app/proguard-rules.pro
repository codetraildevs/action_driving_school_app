# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ─── Crashlytics / Debuggability ───
# Preserve line numbers and source file name for readable Firebase Crashlytics stack traces.
# Without these, crash reports show "Unknown source" for every frame.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Gson / Retrofit Data Model Classes ───
# All model classes use @SerializedName on every field, so R8 can safely
# obfuscate property names while keeping the annotation value accessible
# to Gson's reflection-based deserialization.
-keepclassmembers class com.drivingschoolrwandaapp.models.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers class com.drivingschoolrwandaapp.database.entities.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Additional model packages that are used with Gson/Retrofit
# but are outside the models.entities / models.response / models.request structure
-keepclassmembers class com.drivingschoolrwandaapp.data.models.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.drivingschoolrwandaapp.models.IremboApplication
-keep class com.drivingschoolrwandaapp.models.LocalExamWrapper
-keep class com.drivingschoolrwandaapp.models.LocalExam
-keep class com.drivingschoolrwandaapp.models.LocalQuestion

# Keep enums so Gson can deserialize them via reflection
-keepclassmembers enum * { *; }

# ─── Retrofit Reflection Rules ───
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep the ApiService interface — DO NOT allow obfuscation (methods must keep original names for Retrofit reflection)
-keep interface com.drivingschoolrwandaapp.api.ApiService

# Keep all service method return types (response types) from being obfuscated
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# ─── MapStruct Generated Mappers ───
# Mappers.getMapper() reflectively instantiates the generated *Impl classes
# via their public no-arg constructor. In R8 full mode (android.enableR8.fullMode=true)
# a bare -keep class rule keeps only the class name, so members are stripped and
# the app crashes with NoSuchMethodException at <clinit>. Keep all members.
-keep class com.drivingschoolrwandaapp.models.mappers.** { *; }

# ─── OkHttp dontwarn (optional platform dependencies) ───
# OkHttp tries to load Conscrypt, BouncyCastle, and OpenJSSE on certain Android versions.
# These are optional and can be safely ignored.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ─── PDFBox-Android (optional) ───
-dontwarn com.gemalto.jp2.**
