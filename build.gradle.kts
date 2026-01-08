import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tryagian"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tryagian"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 从local.properties读取腾讯云密钥
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()

        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        buildFeatures {
            dataBinding = true
            viewBinding =true
        }

        buildConfigField(
            "String",
            "TENCENT_SECRET_ID",
            "\"${localProperties.getProperty("TENCENT_SECRET_ID", "")}\""
        )

        buildConfigField(
            "String",
            "TENCENT_SECRET_KEY",
            "\"${localProperties.getProperty("TENCENT_SECRET_KEY", "")}\""
        )
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain( jdkVersion = 21)
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // 网络请求库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON解析
    implementation("org.json:json:20230227")

    // 图片加载库
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}