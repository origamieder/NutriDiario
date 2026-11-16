plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nutridiario.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nutridiario.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.11"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            // ./gradlew assembleRelease -Pemulator  → incluye también x86_64 para el emulador de Android Studio
            if (project.hasProperty("emulator")) abiFilters += listOf("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Firmado con la clave de depuración para que se pueda instalar directamente
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    androidResources { noCompress += "tflite" }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
                it.maxHeapSize = "3g"
                listOf("preprocess.in", "preprocess.out", "live", "ids").forEach { k -> System.getProperty(k)?.let { v -> it.systemProperty(k, v) } }
            }
        }
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Cámara + escáner de códigos de barras (modelo incluido en la app, sin Google Play Services)
    val camerax = "1.4.1"
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    // Lectura de texto de la tabla nutricional (modelo incluido, sin internet)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // IA local para reconocer alimentos en fotos
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    // Modelo de platos (Food-101, EfficientNet-B0) ejecutado con LiteRT
    implementation("com.google.ai.edge.litert:litert:1.4.0")

    // Imágenes de productos
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")
    // Capturas de pantalla de la interfaz en el ordenador (sin emulador)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.json:json:20240303")
}
