# kotlinx.serialization: keep generated serializers for the hub DTOs
-keepclassmembers class com.cometchat.uikit.core.hub.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cometchat.uikit.core.hub.**$$serializer { *; }

# Kit models are populated/serialized reflectively (Gson, JSON metadata)
-keep class com.cometchat.chat.models.** { *; }
