import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Đọc API key từ file .env ở thư mục gốc (xem cấu trúc ở .env.example: API_KEY="...").
// Không commit .env (đã có trong .gitignore). Nếu thiếu key thì build vẫn chạy,
// app sẽ báo lỗi thân thiện lúc runtime.
fun loadEnvApiKey(): String {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return ""
    envFile.readLines().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val eq = line.indexOf('=')
        if (eq <= 0) return@forEach
        val key = line.substring(0, eq).trim()
        if (key == "API_KEY") {
            var value = line.substring(eq + 1).trim()
            // Bỏ dấu nháy đơn/kép bao quanh: API_KEY="xxx" -> xxx
            if ((value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) ||
                (value.startsWith("'") && value.endsWith("'") && value.length >= 2)
            ) {
                value = value.substring(1, value.length - 1)
            }
            if (value.isNotEmpty()) return value
        }
    }
    // Fallback: thử local.properties (tuỳ chọn, cho máy chưa có .env)
    val localProps = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { localProps.load(it) }
        return (localProps.getProperty("weatherApiKey") ?: "").trim()
    }
    return ""
}

val weatherApiKey = loadEnvApiKey()

android {
    namespace = "dev.zr0x8.nt118.gaymiennam"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.zr0x8.nt118.gaymiennam"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject key từ .env vào BuildConfig, code Kotlin đọc qua BuildConfig.WEATHER_API_KEY
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.coil)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
