# Project-specific R8 rules.
#
# The app has no reflection-based serialization: JSON is read by hand with org.json
# (part of the Android framework), so no model classes need -keep rules.

# Keep file names and line numbers so Play Console crash reports stay readable
# once the mapping file is uploaded.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
