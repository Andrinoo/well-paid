import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("androidx.baselineprofile")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use { load(it) }
}

fun normalizeApiBaseUrl(raw: String): String =
    raw.trim().let { if (it.endsWith("/")) it else "$it/" }

/** Debug: emulador ou backend local (`api.base.url` em local.properties). */
val apiBaseUrlDebugOverride = localProperties.getProperty("api.base.url")?.let(::normalizeApiBaseUrl)

/**
 * Release (APK): opcional em local.properties — só para apontar o APK para outro host sem mudar gradle.properties.
 * Prioridade: api.release.base.url → wellpaid.api.release.url (gradle.properties).
 */
val apiBaseUrlReleaseOverride =
    localProperties.getProperty("api.release.base.url")?.let(::normalizeApiBaseUrl)

fun debugApiUrlFromProject(): String {
    val fromGradle =
        (project.findProperty("wellpaid.api.debug.url") ?: rootProject.findProperty("wellpaid.api.debug.url"))
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    // Debug prioriza: local.properties (api.base.url) > gradle.properties (wellpaid.api.debug.url)
    // > release URL para facilitar diagnóstico contra o mesmo backend.
    return normalizeApiBaseUrl(fromGradle ?: releaseApiUrlFromProject())
}

fun releaseApiUrlFromProject(): String {
    val fromGradle =
        (project.findProperty("wellpaid.api.release.url") ?: rootProject.findProperty("wellpaid.api.release.url"))
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error(
                "Defina wellpaid.api.release.url em android-native/gradle.properties (URL do deploy Vercel para o APK release).",
            )
    return normalizeApiBaseUrl(fromGradle)
}

fun escapeBuildConfigString(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.wellpaid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.wellpaid"
        minSdk = 26
        targetSdk = 35
        // Alinha com a revisão Alembic em `head` (ex.: 033 → 0.1.32). Atualizar ao adicionar migrações.
        versionCode = 32
        versionName = "0.1.32"
        val revisionPrefix =
            (project.findProperty("wellpaid.revision.code") as String?)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "AN_CA_RBCCA"
        val buildStamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())
        buildConfigField("String", "REVISION_CODE", "\"${escapeBuildConfigString(revisionPrefix)}\"")
        buildConfigField("String", "BUILD_TIMESTAMP", "\"${escapeBuildConfigString(buildStamp)}\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile")!!)
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            val url = apiBaseUrlDebugOverride ?: debugApiUrlFromProject()
            buildConfigField("String", "API_BASE_URL", "\"${escapeBuildConfigString(url)}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val url = apiBaseUrlReleaseOverride ?: releaseApiUrlFromProject()
            buildConfigField("String", "API_BASE_URL", "\"${escapeBuildConfigString(url)}\"")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    lint {
        // No Windows o lint vital em release pode falhar com "arquivo já em uso" em
        // app/build/.../lint-cache (IDE, daemon Gradle, antivírus). Desliga o lint
        // automático no assembleRelease; corre `./gradlew lint` quando quiseres análise.
        checkReleaseBuilds = false
    }
}

dependencies {
    baselineProfile(project(":baselineprofile"))
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))

    // NetworkModule referencia OkHttpClient/Retrofit; core:network nao exporta estes JARs.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // `compose-bom:2024.12.01` (e até `2025.02.00`) ainda puxam material3 1.3.x, onde
    // `ModalBottomSheet` não expõe `sheetGesturesEnabled`. A partir de material3 1.4.0
    // (BOM `2025.10.01` no nosso resolve) dá para desligar o swipe-to-dismiss sem
    // interferir com o scrim (toque fora).
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
}
