plugins {
    id("com.android.application")
}

android {
    namespace = "com.hook.twitter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hook.twitter"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {}
