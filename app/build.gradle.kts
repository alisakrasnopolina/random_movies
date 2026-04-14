import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.random_movie"
    compileSdk = 33

    defaultConfig {
        val localProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }

        val kinopoiskBaseUrl = localProps.getProperty("KINOPOISK_BASE_URL", "https://api.poiskkino.dev/v1.4")
        val kinopoiskApiKey = localProps.getProperty("KINOPOISK_API_KEY", "0QZTAKB-HX6MTJ1-N6ABCHA-MSF9HBF")

        buildConfigField("String", "KINOPOISK_BASE_URL", "\"$kinopoiskBaseUrl\"")
        buildConfigField("String", "KINOPOISK_API_KEY", "\"$kinopoiskApiKey\"")

        val apiBaseUrl = localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8000")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        applicationId = "com.example.random_movie"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        dataBinding =  true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("com.google.firebase:firebase-database:20.3.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.sun.mail:javax.mail:1.6.2")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}