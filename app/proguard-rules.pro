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
-keep class com.drivingschoolrwandaapp.database.entities.** { *; }

# --- Rules for Retrofit and Gson ---
# Prevent obfuscation of generic types used by Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.TypeAdapter

# Keep the ApiService interface and its methods from being obfuscated
-keep interface com.drivingschoolrwandaapp.api.ApiService { *; }

# --- Rules for MapStruct ---
# If MapStruct generated mappers are used via reflection or need to be kept
-keep class com.drivingschoolrwandaapp.models.mappers.** { *; }
-keep class com.drivingschoolrwandaapp.models.mappers.**Impl { *; }

# --- Rules for PDFBox-Android to ignore missing optional classes ---
-dontwarn com.gemalto.jp2.**
