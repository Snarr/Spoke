import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

val secretsProperties = Properties().apply {
    val file = File(System.getProperty("user.home"), ".gradle/gradle.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

fun signingProp(name: String): String? = System.getenv(name) ?: keystoreProperties.getProperty(name)
fun secretProp(name: String): String = secretsProperties.getProperty(name, "").replace("\"", "")

android {
    namespace = "com.jacobsnarr.spoke"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.jacobsnarr.spoke"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "INDEGO_SECRET", "\"${secretProp("INDEGO_SECRET")}\"")
        buildConfigField("String", "METRO_BIKE_SHARE_SECRET", "\"${secretProp("METRO_BIKE_SHARE_SECRET")}\"")
    }

    val signingStoreFilePath = signingProp("ANDROID_KEYSTORE_PATH") ?: signingProp("storeFile")
    val signingStorePassword = signingProp("ANDROID_KEYSTORE_PASSWORD") ?: signingProp("storePassword")
    val signingKeyAlias = signingProp("ANDROID_KEY_ALIAS") ?: signingProp("keyAlias")
    val signingKeyPassword = signingProp("ANDROID_KEY_PASSWORD") ?: signingProp("keyPassword")
    val hasReleaseSigning = !signingStoreFilePath.isNullOrBlank() &&
        !signingStorePassword.isNullOrBlank() &&
        !signingKeyAlias.isNullOrBlank() &&
        !signingKeyPassword.isNullOrBlank()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(signingStoreFilePath)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.muditaMmd)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}