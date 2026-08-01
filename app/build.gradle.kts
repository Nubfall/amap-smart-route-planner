import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(localProperties::load)

android {
    namespace = "cn.edu.gzhu.amap"
    compileSdk = 36

    defaultConfig {
        applicationId = "cn.edu.gzhu.amap"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["AMAP_API_KEY"] = localProperties.getProperty("AMAP_API_KEY", "")

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
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(files("..\\libs\\AMap3DMap_10.1.600_AMapNavi_10.1.600_AMapSearch_9.7.4_AMapLocation_6.5.1_20251020.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}