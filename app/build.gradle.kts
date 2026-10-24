plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.mypainter.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.mypainter.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("production") {
            val filePath = providers.gradleProperty("signingStoreFile").orNull
            if (!filePath.isNullOrBlank()) {
                storeFile = file(filePath)
                storePassword = providers.gradleProperty("signingStorePassword").orNull
                keyAlias = providers.gradleProperty("signingKeyAlias").orNull
                keyPassword = providers.gradleProperty("signingKeyPassword").orNull
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (providers.gradleProperty("signingStoreFile").isPresent)
                signingConfig = signingConfigs.getByName("production")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.16.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
