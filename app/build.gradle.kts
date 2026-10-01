import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

// Release version counter. Google Play rejects an upload whose versionCode was
// already used, so this is read from version.properties and bumped automatically
// after every release build (see the bumpVersionCode task at the bottom).
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    }
}
val appVersionCode = (versionProps.getProperty("versionCode") ?: "1").trim().toInt()

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.tamaade.ecommerce"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tamaade.ecommerce"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Local Django dev server as seen from the Android emulator.
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000/\"")
            // Plain HTTP only for the local dev server.
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://tamaadeapi-7it5.onrender.com/\"")
            // Release is HTTPS-only.
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

/**
 * Bumps versionCode in version.properties so the *next* release build is unique.
 *
 * It runs after packaging, which means the artifact you just built keeps the code it
 * was configured with (Gradle reads versionCode at configuration time, before any
 * task executes). Build N ships code N and leaves N+1 on disk for build N+1.
 */
tasks.register("bumpVersionCode") {
    description = "Increments versionCode in version.properties for the next release build."
    group = "versioning"
    doLast {
        val next = appVersionCode + 1
        versionProps.setProperty("versionCode", next.toString())
        versionPropsFile.outputStream().use { out ->
            versionProps.store(out, "Auto-incremented after each release build.")
        }
        logger.lifecycle("versionCode: $appVersionCode -> $next (next release build uses $next)")
    }
}

// Only release packaging advances the counter; debug builds must not burn version codes.
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach { finalizedBy("bumpVersionCode") }
