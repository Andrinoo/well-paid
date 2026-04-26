import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
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

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:model"))

    implementation("androidx.core:core-ktx:1.15.0")
    // 1.0.0 não inclui MasterKey; API antiga de EncryptedSharedPreferences.create é diferente.
    implementation("androidx.security:security-crypto:1.1.0")
}
