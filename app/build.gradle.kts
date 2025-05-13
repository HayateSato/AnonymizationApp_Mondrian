plugins {
    alias(libs.plugins.android.application)
    id("com.chaquo.python")
}

android {
    namespace = "com.example.pythoncalculation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pythoncalculation"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        buildFeatures {
            viewBinding = true
        }
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

    flavorDimensions += "pyVersion"
    productFlavors {
        create("py308") { dimension = "pyVersion" }
//        create("py310") { dimension = "pyVersion" }
//        create("py311") { dimension = "pyVersion" }
    }
}

chaquopy {
    productFlavors {
        getByName("py308") { version = "3.8" }
//        getByName("py310") { version = "3.10" }
//        getByName("py311") { version = "3.11" }
    }
    defaultConfig {
        pip {
            install("numpy")
            install("pandas")
            install ("cryptography")
        }
    }
}


dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

}
