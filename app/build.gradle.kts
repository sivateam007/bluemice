plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.bluemice"
    compileSdk {
        version = release(36)
    }

    // Release signing is read from bluemice.properties in the project root so
    // no credentials are committed. Create a keystore with:
    //   keytool -genkey -v -keystore bluemice.jks -keyalg RSA -keysize 2048 -validity 10000 -alias bluemice
    // then fill in the four store.* lines. Until then release builds fall
    // back to the debug key so `assembleRelease` still produces an APK.
    val bluemiceProps = rootProject.file("bluemice.properties")
    val hasReleaseSigning = bluemiceProps.exists() &&
        bluemiceProps.readText().lines().any { it.trim().startsWith("store.file") }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val props = bluemiceProps.readLines()
                    .filter { it.isNotBlank() && !it.trim().startsWith("#") }
                    .associate {
                        val i = it.indexOf('=')
                        it.substring(0, i).trim() to it.substring(i + 1).trim()
                    }
                storeFile = rootProject.file(props["store.file"]!!)
                storePassword = props["store.password"]
                keyAlias = props["key.alias"]
                keyPassword = props["key.password"]
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.bluemice"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
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
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
