pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Sintaxe correta e segura exigida pelo Gradle 8.5+
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "TrackDay Live"
include(":app")
