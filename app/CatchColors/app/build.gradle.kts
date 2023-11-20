plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.catchcolors"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.catchcolors"
        minSdk = 23
        targetSdk = 32
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true //뷰 바인딩 buildFeatures.viewBinding true 로 작성 가능
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")             // Retrofit2
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")       // Retrofit2
    implementation("com.squareup.okhttp3:okhttp:4.9.1")                 // OkHttp 라이브러리
    implementation("com.github.bumptech.glide:glide:4.13.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.13.2")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("org.jsoup:jsoup:1.14.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.github.ismaeldivita:chip-navigation-bar:Tag")
    implementation("com.github.ismaeldivita:chip-navigation-bar:1.4.0")
}