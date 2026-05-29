import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply { rootProject.file("local.properties").inputStream().use { load(it) } }



android {
    namespace = "com.gabojameong.petplace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gabojameong.petplace"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val kakaoNativeKey = localProperties.getProperty("KAKAO_NATIVE_KEY") ?: ""
        buildConfigField("String", "KAKAO_NATIVE_KEY", "\"$kakaoNativeKey\"")
        manifestPlaceholders["KAKAO_NATIVE_KEY"] = kakaoNativeKey

        buildConfigField(
            "String",
            "NAVER_CLIENT_ID",
            "\"" + localProperties.getProperty("NAVER_CLIENT_ID") + "\""
        )
        buildConfigField(
            "String",
            "NAVER_CLIENT_SECRET",
            "\"" + localProperties.getProperty("NAVER_CLIENT_SECRET") + "\""
        )
        buildConfigField(
            "String",
            "NAVER_MAP_CLIENT_ID",
            "\"" + localProperties.getProperty("NAVER_MAP_CLIENT_ID") + "\""
        )
        buildConfigField(
            "String",
            "NAVER_MAP_CLIENT_SECRET",
            "\"" + localProperties.getProperty("NAVER_MAP_CLIENT_SECRET") + "\""
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

    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(libs.androidx.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Kakao Login
    implementation("com.kakao.sdk:v2-user:2.20.6")
    // Naver Login
    implementation("com.navercorp.nid:oauth:5.11.2")
    // Naver Map SDK
    implementation("com.naver.maps:map-sdk:3.23.2")
    // Google Play Services Location (네이버맵에 필요함)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Retrofit (Java/Kotlin RESTful API 맞추기용)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Glider (네트워크로 이미지 불러오기용 라이브러리)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.github.yalantis:ucrop:2.2.8")
}