plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hamer.pico2dock"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.hamer.pico2dock"
        minSdk = 29
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(files("C:\\Users\\chai5\\Downloads\\Compressed\\APKEditor-1.4.8.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

//    implementation("com.github.REAndroid:APKEditor:V1.4.6")
    implementation("com.github.TutorialsAndroid:FilePicker:v9.0.1")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("com.github.timscriptov:apksigner:1.2.0")
}