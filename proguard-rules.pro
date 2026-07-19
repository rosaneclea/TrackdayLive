# Protege a tela principal de ser apagada pelo ofuscador
-keep class com.trackday.live.MainActivity { *; }

# Protege as bibliotecas do Google Play Services (GPS)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
