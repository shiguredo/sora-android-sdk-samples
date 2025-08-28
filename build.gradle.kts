plugins {
    alias(libs.plugins.versions) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.android.maven) apply false
}

buildscript {
    // デバッグ用: true に設定すると wss ではなく ws で接続できる
    extra["usesCleartextTraffic"] = false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// 不安定バージョンを除外する設定
fun isNonStable(version: String): Boolean {
    val qualifiers = listOf("alpha", "beta", "rc")
    return qualifiers.any { qualifier ->
        version.matches(Regex("(?i).*[.-]$qualifier[.\\d-]*"))
    }
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection {
            all {
                if (isNonStable(candidate.version)) {
                    reject("Release candidate")
                }
            }
        }
    }
}