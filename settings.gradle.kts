pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "sora-android-sdk-samples"
include(":samples")

// ローカル SDK を composite build で取り込む場合は、
// gradle.properties の `soraSdkDirPath` または環境変数 `SORA_SDK_DIR` を設定した上で以下をアンコメントする
val soraSdkDirPath = providers.gradleProperty("soraSdkDirPath").orNull?.trim()
    ?: System.getenv("SORA_SDK_DIR")?.trim()

if (!soraSdkDirPath.isNullOrBlank() && file(soraSdkDirPath).isDirectory) {
    includeBuild(soraSdkDirPath!!) {
        dependencySubstitution {
            substitute(module("com.github.shiguredo:sora-android-sdk")).using(project(":sora-android-sdk"))
            substitute(module("com.github.shiguredo:shiguredo-webrtc-android")).using(project(":local-libwebrtc"))
        }
    }
}
