plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget
                .fromTarget(libs.versions.jvmTarget.get()),
        )
    }
}

android {
    namespace = "jp.shiguredo.sora.sample"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "jp.shiguredo.sora.sample"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"

        // アプリで参照する設定を BuildConfig / resource に書き込む
        val signalingEndpoint = project.properties["signaling_endpoint"] as? String ?: ""
        val channelId = project.properties["channel_id"] as? String ?: ""
        val signalingMetadata = project.properties["signaling_metadata"] as? String ?: ""
        val proxyAgent = project.properties["proxy_agent"] as? String ?: ""
        val proxyHostname = project.properties["proxy_hostname"] as? String ?: ""
        val proxyPort = project.properties["proxy_port"] as? String ?: ""
        val proxyUsername = project.properties["proxy_username"] as? String ?: ""
        val proxyPassword = project.properties["proxy_password"] as? String ?: ""

        buildConfigField("String", "SIGNALING_ENDPOINT", "\"$signalingEndpoint\"")
        resValue("string", "channelId", "\"$channelId\"")
        buildConfigField("String", "SIGNALING_METADATA", "\"$signalingMetadata\"")

        // プロキシの設定
        buildConfigField("String", "PROXY_AGENT", "\"$proxyAgent\"")
        buildConfigField("String", "PROXY_HOSTNAME", "\"$proxyHostname\"")
        buildConfigField("String", "PROXY_PORT", "\"$proxyPort\"")
        buildConfigField("String", "PROXY_USERNAME", "\"$proxyUsername\"")
        buildConfigField("String", "PROXY_PASSWORD", "\"$proxyPassword\"")

        manifestPlaceholders["usesCleartextTraffic"] = rootProject.extra["usesCleartextTraffic"] as Boolean
    }

    buildFeatures {
        viewBinding = true
        compose = true
        // AGP 8.0 からデフォルトで false になった
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaCompatibility.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaCompatibility.get())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

configurations {
    // assembleRelease などで次のエラーが発生するワークアラウンド
    // "Multiple dex files define Lorg/intellij/lang/annotations/Identifier"
    implementation {
        exclude(group = "org.jetbrains", module = "annotations")
    }
}

ktlint {
    // 設定フェーズでは動的解決や Version Catalog を使えないため固定
    version.set("1.7.1")
    android.set(false)
    outputToConsole.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
    ignoreFailures.set(false)
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.kotlin.reflect)

    implementation(libs.gson)

    implementation(libs.bundles.androidx.ui)
    implementation(libs.bundles.androidx.navigation)
    implementation(libs.bundles.compose)

    implementation(libs.material)
    implementation(libs.material.spinner)

    // Sora Android SDK
    if (findProject(":sora-android-sdk") != null) {
        // module is included
        api(project(":sora-android-sdk"))
    } else {
        // external dependency
        implementation(libs.sora.android.sdk)
    }
}

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(0, "seconds")
        cacheChangingModulesFor(0, "seconds")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    finalizedBy("ktlintFormat")
}
