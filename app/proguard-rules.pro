# R8 optimization and obfuscation for release builds.
# Keep runtime-visible annotations and generic signatures used by libraries.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve enum methods used by serialization / UI labels.
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep model fields annotated for Moshi reflection, if present.
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Avoid leaking original Kotlin source filenames in stack traces.
-renamesourcefileattribute SourceFile
