plugins {
    id("com.android.application") version "8.3.0"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    // Musi zgadzać się z tym, co wpisaliśmy w funkcji JNI w Ruście!
    namespace = "com.im_a_hero.daemon" 
    compileSdk = 34

    defaultConfig {
        applicationId = "com.im_a_hero.daemon"
        minSdk = 26 // Android 8.0 - wystarczy dla naszych usług
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Na razie nie ładujemy tu żadnych ciężkich bibliotek UI.
    // Jedziemy na czystym, surowym Android SDK.
}