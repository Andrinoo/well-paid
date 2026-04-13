plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wellpaid.core.datastore"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))

    implementation("androidx.core:core-ktx:1.15.0")
    // 1.0.0 não inclui MasterKey; API antiga de EncryptedSharedPreferences.create é diferente.
    implementation("androidx.security:security-crypto:1.1.0")
}
