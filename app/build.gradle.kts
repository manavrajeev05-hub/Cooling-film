plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.coolingfilmmonitor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.coolingfilmmonitor"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.13.0") // single, latest version
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
