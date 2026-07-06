# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Rules for Data Models (Entities) ---
# Keep all data classes to prevent Gson mapping issues in Release builds
-keep class com.drivingschoolrwandaapp.models.entities.** { *; }
-keep class com.drivingschoolrwandaapp.models.response.** { *; }
-keep class com.drivingschoolrwandaapp.models.request.** { *; }
-keep class com.drivingschoolrwandaapp.database.entities.** { *; }

# --- Rules for Retrofit and Gson ---
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# CRITICAL: Keep the Call interface name INTACT (NOT obfuscated) so Retrofit can resolve generic types
# If Call is obfuscated (e.g., renamed to ic.c), Call<LoginResponse> loses its generic parameter
# Must use plain -keep (NOT -keep,allowobfuscation) to prevent renaming
-keep interface retrofit2.Call
-keep class retrofit2.Response
-keep class kotlin.coroutines.Continuation

# Keep the ApiService interface — DO NOT allow obfuscation (methods must keep original names for Retrofit reflection)
-keep interface com.drivingschoolrwandaapp.api.ApiService

# Keep all service method return types (response types) from being obfuscated
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}

# Gson specific
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.TypeAdapter
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# --- Rules for MapStruct ---
# If MapStruct generated mappers are used via reflection or need to be kept
-keep class com.drivingschoolrwandaapp.models.mappers.** { *; }
-keep class com.drivingschoolrwandaapp.models.mappers.**Impl { *; }

# --- Additional model packages used with Gson/Retrofit ---
# These are NOT covered by the models.entities/response/request rules above
# because they're in different package paths
-keep class com.drivingschoolrwandaapp.data.models.** { *; }

# Keep models used with Gson for API deserialization (IremboApplication, LocalExam, etc.)
-keep class com.drivingschoolrwandaapp.models.IremboApplication { *; }
-keep class com.drivingschoolrwandaapp.models.LocalExamWrapper { *; }
-keep class com.drivingschoolrwandaapp.models.LocalExam { *; }
-keep class com.drivingschoolrwandaapp.models.LocalQuestion { *; }

# --- OkHttp dontwarn rules for optional platform dependencies ---
# OkHttp tries to load Conscrypt, BouncyCastle, and OpenJSSE on certain Android versions
# These are optional and can be safely ignored
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Rules for PDFBox-Android to ignore missing optional classes ---
-dontwarn com.gemalto.jp2.**
