import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt") // 【新增】必须添加，用于 Glide 注解处理
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

        // 【修正】移除了原本错误放在这里的 buildFeatures 块
        
        // 注入 BuildConfig 字段
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
        // 【建议】如果环境不支持 Java 21，请改回 VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17 
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17" // 【修正】Kotlin DSL 中通常用 kotlinOptions 或 jvmToolchain，确保与上面一致
    }
    
    // 或者保留你原来的写法，但要确保 JDK 版本一致
    // kotlin {
    //     jvmToolchain(17)
    // }

    buildFeatures {
        // 【修正】统一在这里配置
        buildConfig = true // 必须开启，否则无法使用 BuildConfig 类
        viewBinding = true
        // dataBinding = true // 如果没用到 XML 双向绑定，建议关闭以加快编译
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

    // JSON解析 (Android 自带 org.json，通常不需要额外引入，除非需要特定新版本特性)
    // implementation("org.json:json:20230227") // 可选，Android SDK 已内置

    // 图片加载库
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0") // 【修正】改用 kapt

    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // 【建议补充】Lifecycle Scope，解决你之前协程内存泄漏问题
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2") 

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
