plugins {
    alias(libs.plugins.versions) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
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

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    resolutionStrategy {
        componentSelection {
            all {
                // 不安定バージョンを除外する設定
                val isNonStable = listOf("alpha", "beta", "rc").any { qualifier ->
                    candidate.version.matches(Regex("(?i).*[.-]$qualifier[.\\d-]*"))
                }
                if (isNonStable) {
                    reject("Release candidate")
                }
            }
        }
    }
}
